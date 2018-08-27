package codesearch.core.index

import java.io.File

import ammonite.ops.{Path, pwd}

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.index.LanguageIndex.{CSearchPage, ContentByURI, PackageResult, SearchArguments}
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

  def csearch(args: SearchArguments, page: Int): Future[CSearchPage] = {
    runCsearch(args).map { answers =>
      val data = answers
        .slice(math.max(page - 1, 0) * LanguageIndex.PAGE_SIZE, page * LanguageIndex.PAGE_SIZE)
        .flatMap(contentByURI)
        .groupBy { x =>
          (x.name, x.url)
        }
        .map {
          case ((verName, packageLink), results) =>
            PackageResult(verName, packageLink, results.map(_.result).toSeq)
        }
        .toSeq
        .sortBy(_.name)
      CSearchPage(data, answers.length)
    }
  }

  protected def contentByURI(uri: String): Option[ContentByURI]

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

  protected def runCsearch(arg: SearchArguments): Future[Array[String]] = {
    val pathRegex = {
      if (arg.sourcesOnly) {
        langExts
      } else {
        ".*"
      }
    }

    val query: String = {
      if (arg.preciseMatch) {
        Helper.hideSymbols(arg.query)
      } else {
        arg.query
      }
    }

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (arg.insensitive) {
      args.append("-i")
    }
    args.append("-f", pathRegex)
    args.append(query)
    logger.debug(indexPath.toString())

    Future {
      (Process(args, None, "CSEARCHINDEX" -> indexPath.toString()) #| Seq("head", "-1001")).!!.split('\n')
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

object LanguageIndex {
  case class CSearchPage(data: Seq[PackageResult], total: Int)
  case class Result(fileLink: String, firstLine: Int, nLine: Int, ctxt: Seq[String])
  case class PackageResult(name: String, packageLink: String, results: Seq[Result])
  final case class SearchArguments(query: String, insensitive: Boolean, preciseMatch: Boolean, sourcesOnly: Boolean)
  final case class ContentByURI(name: String, url: String, result: Result)

  val PAGE_SIZE = 100
}
