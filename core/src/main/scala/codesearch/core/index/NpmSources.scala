package codesearch.core.index

import java.net.URLEncoder

import ammonite.ops.{Path, pwd}
import codesearch.core.db.DefaultDB
import codesearch.core.index.HackageSources.{downloadFile, indexAPI, logger, runCsearch}

import scala.sys.process._
import codesearch.core.model.NpmTable
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

object NpmSources extends Sources[NpmTable] {
  override val logger: Logger = LoggerFactory.getLogger(NpmSources.getClass)
  private val SOURCES: Path = pwd / 'data / 'js / 'packages

  private val extensions: Set[String] = Set("js", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")

  override protected val indexAPI: Index with DefaultDB[NpmTable] = NpmIndex
  override val indexFile: String = ".npm_csearch_index"

  override val langExts: String = ".*\\.(js|json)$"

  private var counter: Int = 0

  def csearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean, page: Int): (Int, Seq[PackageResult]) = {
    val answers = runCsearch(searchQuery, insensitive, precise, sources)
    (answers.length, answers
      .slice(math.max(page - 1, 0) * 100, page * 100)
      .flatMap(NpmIndex.contentByURI).groupBy { x => (x._1, x._2) }.map {
      case ((verName, packageLink), results) =>
        PackageResult(verName, packageLink, results.map(_._3).toSeq)
    }.toSeq.sortBy(_.name))
  }

  override def downloadSources(name: String, ver: String): Future[Int] = {
    counter += 1
    if (counter == 100) {
      counter = 0
      Thread.sleep(10000)
    }
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
      val result = archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir, Some(extensions))
      result
  }
}
