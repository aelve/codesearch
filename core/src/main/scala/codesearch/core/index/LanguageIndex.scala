package codesearch.core.index

import java.io.File

import ammonite.ops.{Path, pwd}

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.{DefaultTable, Version}
import codesearch.core.util.Helper
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait LanguageIndex[VTable <: DefaultTable] { self: DefaultDB[VTable] =>
  protected val logger: Logger
  protected val langExts: String

  protected val indexFile: String
  protected lazy val indexPath: Path = pwd / 'data / indexFile

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def downloadMetaInformation(): Unit

  /**
    * download all latest packages version
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
              downloadSources(packageName, currentVersion)
          })
        }
      }
      .map(_.sum)
  }

  protected implicit def executor: ExecutionContext

  protected def archiveDownloadAndExtract(name: String,
                                          ver: String,
                                          packageURL: String,
                                          packageFileGZ: Path,
                                          packageFileDir: Path,
                                          extensions: Option[Set[String]] = None,
                                          extractor: (String, String) => Unit = defaultExtractor): Future[Int] = {

    val archive     = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    try {
      destination.mkdirs()

      Seq("curl", "-o", archive.getCanonicalPath, packageURL) !!

      extractor(archive.getCanonicalPath, destination.getCanonicalPath)

      if (extensions.isDefined) {
        applyFilter(extensions.get, archive)
        applyFilter(extensions.get, destination)
      }

      insertOrUpdate(name, ver)
    } catch {
      case e: Exception =>
        logger.debug(e.getLocalizedMessage)
        Future {
          0
        }
    }
  }

  protected def runCsearch(searchQuery: String,
                           insensitive: Boolean,
                           precise: Boolean,
                           sources: Boolean): Array[String] = {
    val pathRegex = {
      if (sources) {
        langExts
      } else {
        ".*"
      }
    }

    val query: String = {
      if (precise) {
        Helper.hideSymbols(searchQuery)
      } else {
        searchQuery
      }
    }

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (insensitive) {
      args.append("-i")
    }
    args.append("-f", pathRegex)
    args.append(query)
    logger.debug(indexPath.toString())

    val answer = (Process(args, None, "CSEARCHINDEX" -> indexPath.toString()) #| Seq("head", "-1001")).!!

    if (answer.nonEmpty) {
      answer.split('\n')
    } else {
      Array()
    }
  }

  /**
    * Collect last versions of packages in local folder
    * Key for map is package name, value is last version
    * @return last versions of packages
    */
  protected def getLastVersions: Map[String, Version]

  /**
    * download source code from remote repository
    * @param name of package
    * @param ver of package
    * @return count of downloaded files (source files)
    */
  protected def downloadSources(name: String, ver: String): Future[Int]

  protected def applyFilter(extensions: Set[String], curFile: File): Unit = {
    if (curFile.isDirectory) {
      curFile.listFiles.foreach(applyFilter(extensions, _))
    } else {
      val ext = FilenameUtils.getExtension(curFile.getName)
      if (curFile.exists() && !(extensions contains ext)) {
        curFile.delete
      }
    }
  }

  private lazy val defaultExtractor: (String, String) => Unit =
    (src: String, dst: String) => Seq("tar", "-xvf", src, "-C", dst) !!
}
