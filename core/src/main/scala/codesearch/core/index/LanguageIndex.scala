package codesearch.core.index

import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Path => NioPath}

import cats.effect.{ContextShift, IO}
import cats.instances.int._
import cats.syntax.functor._
import codesearch.core.db.DefaultDB
import codesearch.core.index.directory.{Directory, СSearchDirectory}
import codesearch.core.index.repository._
import codesearch.core.model.DefaultTable
import fs2.Stream
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.sys.process._

trait LanguageIndex[A <: DefaultTable] { self: DefaultDB[A] =>

  protected implicit def shift: ContextShift[IO]

  protected val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.unsafeCreate[IO]

  protected def csearchDir: СSearchDirectory

  protected def concurrentTasksCount: Int

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def downloadMetaInformation: IO[Unit]

  /**
    * Build new index from only latest version of each package and
    * replace old index with new one.
    *
    * @return cindex exit code
    */
  def buildIndex: IO[Int] = {
    def latestPackagePaths = verNames.map { versions =>
      versions.map {
        case (packageName, version) =>
          buildFsUrl(packageName, version)
      }
    }

    def dropTempIndexFile = IO(Files.deleteIfExists(csearchDir.tempIndexDirAs[NioPath]))

    def createCSearchDir = IO(
      if (Files.notExists(СSearchDirectory.root))
        Files.createDirectories(СSearchDirectory.root)
    )

    def indexPackages(packageDirs: Seq[NioPath]) = IO {
      val env  = Seq("CSEARCHINDEX" -> csearchDir.tempIndexDirAs[String])
      val args = "cindex" +: packageDirs.map(_.toString)
      Process(args, None, env: _*) !
    }

    def replaceIndexFile = IO(
      Files.move(
        csearchDir.tempIndexDirAs[NioPath],
        csearchDir.indexDirAs[NioPath],
        REPLACE_EXISTING
      )
    )

    for {
      packageDirs <- latestPackagePaths
      _           <- createCSearchDir
      _           <- dropTempIndexFile
      exitCode    <- indexPackages(packageDirs)
      _           <- replaceIndexFile
    } yield exitCode
  }

  /**
    * download all latest packages version
    *
    * @return count of updated packages
    */
  def updatePackages: IO[Int] = {
    for {
      _           <- logger.debug("UPDATE PACKAGES")
      packagesMap <- verNames.map(_.toMap)
      packagesCount <- getLastVersions.filter {
        case (packageName, currentVersion) =>
          !packagesMap.get(packageName).contains(currentVersion)
      }.mapAsyncUnordered(concurrentTasksCount)(updateSources _ tupled).compile.foldMonoid
    } yield packagesCount
  }

  /**
    * Create path to package in file system
    *
    * @param packageName local package name
    * @param version of package
    * @return path
    */
  protected def buildFsUrl(packageName: String, version: String): NioPath

  protected def archiveDownloadAndExtract[B <: SourcePackage: Directory](pack: B)(
      implicit repository: SourcesDownloader[IO, B]
  ): IO[Int] = {
    val task = for {
      _         <- repository.downloadSources(pack)
      rowsCount <- insertOrUpdate(pack)
    } yield rowsCount
    task.handleErrorWith(ex => logger.error(ex)(s"Error while downloading $pack") as 0)
  }

  /**
    * Collect last versions of packages in local folder
    * Key for map is package name, value is last version
    *
    * @return last versions of packages
    */
  protected def getLastVersions: Stream[IO, (String, String)]

  /**
    * Update source code from remote repository
    *
    * @see [[https://github.com/aelve/codesearch/wiki/Codesearch-developer-Wiki#updating-packages]]
    * @param name of package
    * @param version of package
    * @return count of downloaded files (source files)
    */
  protected def updateSources(name: String, version: String): IO[Int]
}
