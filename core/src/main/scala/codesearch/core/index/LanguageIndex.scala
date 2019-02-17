package codesearch.core.index

import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Path => NioPath}

import cats.effect.{ContextShift, IO}
import cats.instances.int._
import cats.instances.vector._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.monadError._
import codesearch.core.db.DefaultDB
import codesearch.core.index.directory.{Directory, СSearchDirectory}
import codesearch.core.index.repository._
import codesearch.core.model.DefaultTable
import codesearch.core.syntax.stream._
import fs2.Stream
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.sys.process.Process

trait LanguageIndex[A <: DefaultTable] {
  self: DefaultDB[A] =>

  protected implicit def shift: ContextShift[IO]

  protected val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.unsafeCreate[IO]

  protected def csearchDir: СSearchDirectory

  protected def concurrentTasksCount: Int

  /**
    * Build new index from only latest version of each package and
    * replace old index with new one.
    */
  def buildIndex: IO[Unit] = {
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

    def indexPackages(packageDirs: Seq[NioPath]): IO[Unit] = {
      def cindex(packages: Seq[NioPath]) = {
        val args = "cindex" +: packages.map(_.toString)
        val env  = Seq("CSEARCHINDEX" -> csearchDir.tempIndexDirAs[String])
        IO { Process(args, None, env: _*) ! }
          .ensureOr(BadExitCode)(_ == 0)
      }

      val batchSize = 100000
      packageDirs.grouped(batchSize).toVector.traverse_(cindex)
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
      _           <- indexPackages(packageDirs)
      _           <- replaceIndexFile
    } yield ()
  }

  /**
    * download all latest packages version
    *
    * @return count of updated packages
    */
  def updatePackages(limit: Option[Int]): IO[Int] = {
    val packages: Stream[IO, (String, String)] = getLastVersions.filterNotM {
      case (packageName, packageVersion) => packageIsExists(packageName, packageVersion)
    }

    logger.debug("UPDATE PACKAGES") >> limit
      .map(packages.take(_))
      .getOrElse(packages)
      .mapAsyncUnordered(concurrentTasksCount)(updateSources _ tupled)
      .compile
      .foldMonoid
  }

  /**
    * Create path to package in file system
    *
    * @param packageName local package name
    * @param version     of package
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
    * @param name    of package
    * @param version of package
    * @return count of downloaded files (source files)
    */
  protected def updateSources(name: String, version: String): IO[Int]
}

case class BadExitCode(code: Int) extends Exception(s"Process returned a bad exit code: $code")
