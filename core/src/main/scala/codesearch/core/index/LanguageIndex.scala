package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Path => NioPath}

import ammonite.ops.{Path, pwd}
import cats.effect.{ContextShift, IO}
import cats.instances.int._
import cats.syntax.functor._
import codesearch.core.db.DefaultDB
import codesearch.core.index.LanguageIndex.ConcurrentTasksCount
import codesearch.core.index.directory.Directory
import codesearch.core.index.repository._
import codesearch.core.model.{DefaultTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.sys.process._

trait LanguageIndex[VTable <: DefaultTable] { self: DefaultDB[VTable] =>

  protected implicit def executor: ExecutionContext
  protected implicit def shift: ContextShift[IO]
  protected implicit def http: SttpBackend[IO, Stream[IO, ByteBuffer]]

  protected val logger = Slf4jLogger.unsafeCreate[IO]

  protected val langExts: String
  protected val indexFile: String
  protected lazy val indexPath: Path     = pwd / 'data / indexFile
  protected lazy val tempIndexPath: Path = pwd / 'data / s"$indexFile.tmp"

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

    def dropTempIndexFile = IO(Files.deleteIfExists(tempIndexPath.toNIO))

    def indexPackages(packageDirs: Seq[NioPath]) = IO {
      val env  = Seq("CSEARCHINDEX" -> tempIndexPath.toString)
      val args = "cindex" +: packageDirs.map(_.toString)
      Process(args, None, env: _*) !
    }

    def replaceIndexFile = IO(Files.move(tempIndexPath.toNIO, indexPath.toNIO, REPLACE_EXISTING))

    for {
      packageDirs <- latestPackagePaths
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

      //TODO: getLastVersions should return fs2.Stream[F, (String, Version)]
      packagesCount <- Stream
        .fromIterator[IO, (String, String)](getLastVersions.mapValues(_.verString).iterator)
        .filter {
          case (packageName, currentVersion) =>
            !packagesMap.get(packageName).contains(currentVersion)
        }
        .mapAsyncUnordered(ConcurrentTasksCount)(updateSources _ tupled)
        .compile
        .foldMonoid
    } yield packagesCount
  }

  /**
    * Create link to remote repository.
    *
    * @param packageName local package name
    * @param version of package
    * @return link
    */
  protected def buildRepUrl(packageName: String, version: String): String

  /**
    * Create path to package in file system
    *
    * @param packageName local package name
    * @param version of package
    * @return path
    */
  protected def buildFsUrl(packageName: String, version: String): NioPath

  protected def archiveDownloadAndExtract[A <: SourcePackage: Extensions: Directory](pack: A): IO[Int] = {
    val repository = SourceRepository[A](new FileDownloader())
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
  protected def getLastVersions: Map[String, Version]

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

object LanguageIndex {
  val ConcurrentTasksCount = 30
}
