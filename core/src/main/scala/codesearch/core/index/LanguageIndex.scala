package codesearch.core.index

import java.io.File

import ammonite.ops.{Path, root}

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.{DefaultTable, Version}
import codesearch.core.util.Helper
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait LanguageIndex[VTable <: DefaultTable] {
  protected val indexAPI: Index with DefaultDB[VTable]
  protected val logger: Logger
  protected val langExts: String

  protected val indexFile: String
  protected lazy val indexPath: Path = root / 'root / 'aelve / 'data / indexFile // FIXME

  def downloadMetaInformation(): Unit = indexAPI.updateIndex()

  def getLastVersions: Map[String, Version] = indexAPI.getLastVersions

  def downloadSources(name: String, ver: String): Future[Int]

  implicit def executor: ExecutionContext

  def updatePackages(): Future[Int] = {
    Future
      .successful(logger.debug("UPDATE PACKAGES"))
      .map(_ => getLastVersions)
      .map(_.mapValues(_.verString))
      .flatMap { versions =>
        indexAPI.verNames().flatMap { packages =>
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

  lazy val defaultExtractor: (String, String) => Unit =
    (src: String, dst: String) => Seq("tar", "-xvf", src, "-C", dst) !!

  def archiveDownloadAndExtract(name: String,
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

      indexAPI.insertOrUpdate(name, ver)
    } catch {
      case e: Exception =>
        logger.debug(e.getLocalizedMessage)
        Future {
          0
        }
    }
  }

  def applyFilter(extensions: Set[String], curFile: File): Unit = {
    if (curFile.isDirectory) {
      curFile.listFiles.foreach(applyFilter(extensions, _))
    } else {
      val ext = FilenameUtils.getExtension(curFile.getName)
      if (curFile.exists() && !(extensions contains ext)) {
        curFile.delete
      }
    }
  }

  def runCsearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean): Array[String] = {
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
}
