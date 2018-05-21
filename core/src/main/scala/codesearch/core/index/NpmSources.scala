package codesearch.core.index

import ammonite.ops.{Path, pwd}
import codesearch.core.db.DefaultDB
import codesearch.core.index.HackageSources.{downloadFile, indexAPI, logger}

import scala.sys.process._
import codesearch.core.model.NpmTable
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

object NpmSources extends Sources[NpmTable] {
  private val logger: Logger = LoggerFactory.getLogger(CratesSources.getClass)
  private val SOURCES: Path = pwd / 'data / 'js / 'packages

  override protected val indexAPI: Index with DefaultDB[NpmTable] = NpmIndex

  override def downloadSources(name: String, ver: String): Future[Int] = {
    SOURCES.toIO.mkdirs()

    s"rm -rf ${SOURCES / name}" !!

    val packageURL =
      s"https://registry.npmjs.org/$name/-/$name-$ver.tgz"

    val packageFileGZ =
      pwd / 'data / 'js / 'packages / name / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'js / 'packages / name / ver

    logger.debug(s"EXTRACTING $name-$ver")
    val result = archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir)
    logger.debug("EXTRACTED")
    result
  }
}
