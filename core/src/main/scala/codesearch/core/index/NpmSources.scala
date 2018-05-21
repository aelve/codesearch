package codesearch.core.index

import java.net.URLEncoder

import ammonite.ops.{Path, pwd}
import codesearch.core.db.DefaultDB
import codesearch.core.index.HackageSources.{downloadFile, indexAPI, logger, runCsearch}

import scala.sys.process._
import codesearch.core.model.NpmTable
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

object NpmSources extends Sources[NpmTable] {
  private val logger: Logger = LoggerFactory.getLogger(CratesSources.getClass)
  private val SOURCES: Path = pwd / 'data / 'js / 'packages

  override protected val indexAPI: Index with DefaultDB[NpmTable] = NpmIndex

  def csearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean, page: Int): (Int, Seq[PackageResult]) = {
    val pathRegex = {
      if (sources) {
        ".*\\.(js)$"
      } else {
        "*"
      }
    }
    val answer = runCsearch(searchQuery, insensitive, precise, pathRegex)
    val answers = answer.split('\n')
    (answers.length, answers
      .slice(math.max(page - 1, 0) * 100, page * 100)
      .flatMap(NpmIndex.contentByURI).groupBy { x => (x._1, x._2) }.map {
      case ((verName, packageLink), results) =>
        PackageResult(verName, packageLink, results.map(_._3).toSeq)
    }.toSeq.sortBy(_.name))
  }

  override def downloadSources(name: String, ver: String): Future[Int] = {
    val encodedName = URLEncoder.encode(name, "UTF-8")
    SOURCES.toIO.mkdirs()

    s"rm -rf ${SOURCES / encodedName}" !!

    val packageURL =
      s"https://registry.npmjs.org/$name/-/$name-$ver.tgz"

    val packageFileGZ =
      pwd / 'data / 'js / 'packages / encodedName / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'js / 'packages / encodedName / ver

    logger.info(s"EXTRACTING $name-$ver (dir: $encodedName)")
    val result = archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir)
    logger.info("EXTRACTED")
    result
  }
}
