package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Path => NioPath}

import ammonite.ops.{Path, pwd}
import cats.effect.IO
import codesearch.core.db.DefaultDB
import codesearch.core.index.directory.Directory
import codesearch.core.index.repository._
import codesearch.core.model.{DefaultTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

trait LanguageIndex[VTable <: DefaultTable] { self: DefaultDB[VTable] =>

  protected implicit def executor: ExecutionContext
  protected implicit def http: SttpBackend[IO, Stream[IO, ByteBuffer]]

  protected val logger: Logger

  protected val langExts: String
  protected val indexFile: String
  protected lazy val indexPath: Path     = pwd / 'data / indexFile
  protected lazy val tempIndexPath: Path = pwd / 'data / s"$indexFile.tmp"

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def downloadMetaInformation(): Unit

  /**
    * Build new index from only latest version of each package and
    * replace old index with new one.
    *
    * @return cindex exit code
    */
  def buildIndex(): Future[Int] = {
    def latestPackagePaths = verNames().map { versions =>
      versions.map {
        case (packageName, version) =>
          buildFsUrl(packageName, version)
      }
    }

    def dropTempIndexFile = Future(Files.deleteIfExists(tempIndexPath.toNIO))

    def indexPackages(packageDirs: Seq[NioPath]) = Future {
      val env  = Seq("CSEARCHINDEX" -> tempIndexPath.toString)
      val args = "cindex" +: packageDirs.map(_.toString)
      Process(args, None, env: _*) !
    }

    def replaceIndexFile = Future(Files.move(tempIndexPath.toNIO, indexPath.toNIO, REPLACE_EXISTING))

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
  def updatePackages(): Future[Int] = {
    Future
      .successful(logger.debug("UPDATE PACKAGES"))
      .map(_ => getLastVersions)
      .map(_.mapValues(_.verString))
      .flatMap { versions =>
        verNames().flatMap { packages =>
          val packagesMap = Map(packages: _*)
          Future.sequence(versions.filter {
            case (packageName, currentVersion) =>
              !packagesMap.get(packageName).contains(currentVersion)
          }.map {
            case (packageName, currentVersion) =>
              updateSources(packageName, currentVersion)
          })
        }
      }
      .map(_.sum)
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

  protected def archiveDownloadAndExtract[A <: SourcePackage: Extensions: Directory](pack: A): Future[Int] = {
    val repository = SourceRepository[A](new FileDownloader())
    (for {
      _         <- repository.downloadSources(pack).unsafeToFuture()
      rowsCount <- insertOrUpdate(pack)
    } yield rowsCount).recover { case ex => logger.error(s"Error while downloading $pack", ex); 0 }
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
  protected def updateSources(name: String, version: String): Future[Int]
}
