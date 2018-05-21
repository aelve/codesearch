package codesearch.core.index

import java.io.File

import ammonite.ops.Path

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Sources[VTable <: DefaultTable] {
  protected val indexAPI: Index with DefaultDB[VTable]

  def downloadSources(name: String, ver: String): Future[Int]

  def update(): Future[Int] = {
    val lastVersions = indexAPI.getLastVersions.mapValues(_.verString)

    val futureAction = indexAPI.verNames().flatMap { packages =>
      val packagesMap = Map(packages: _*)
      Future.sequence(lastVersions.filter {
        case (packageName, currentVersion) =>
          !packagesMap.get(packageName).contains(currentVersion)
      }.map {
        case (packageName, currentVersion) =>
          downloadSources(packageName, currentVersion)
      })
    }.map(_.sum)

    futureAction
  }

  def archiveDownloadAndExtract(name: String, ver: String, packageURL: String,
                                packageFileGZ: Path,
                                packageFileDir: Path): Future[Int] = {

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    destination.mkdirs()

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    try {
      downloadFile(packageURL, archive)
//      logger.info(s"downloaded")

      archiver.extract(archive, destination)
//      logger.info("extacted")

      val future = indexAPI.insertOrUpdate(name, ver)
//      logger.info("DB updated")

      future
    } catch {
      case e: Exception =>
        Future[Int] {
//          logger.info(e.getMessage)
          0
        }
    }

  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
    s"curl -o ${dstFile.getPath} $srcURL" !!
  }

  def runCsearch(searchQuery: String,
                 insensitive: Boolean, precise: Boolean, pathRegex: String) = {
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
    (args #| Seq("head", "-1001")).!!
  }
}
