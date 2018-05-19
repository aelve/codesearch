package codesearch.core.index

import java.io.{File, IOException}

import ammonite.ops.pwd
import codesearch.core.model.HackageTable

import scala.collection.mutable
import sys.process._
import codesearch.core.util.Helper
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class Result(fileLink: String, firstLine: Int, nLine: Int, ctxt: Seq[String])
case class PackageResult(name: String, packageLink: String, results: Seq[Result])

object HackageSources extends Sources[HackageTable] {
  private val logger: Logger = LoggerFactory.getLogger(HackageSources.getClass)
  override val indexAPI: HackageIndex.type = HackageIndex

  def csearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean, page: Int): (Int, Seq[PackageResult]) = {
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
    if (sources) {
      args.append("-f", ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$")
    }
    args.append(query)

    val answer = (args #| Seq("head", "-1001")).!!
    val answers = answer.split('\n')
    (answers.length, answers
      .slice(math.max(page - 1, 0) * 100, page * 100)
      .flatMap(HackageIndex.contentByURI).groupBy { x => (x._1, x._2) }.map {
      case ((verName, packageLink), results) =>
        PackageResult(verName, packageLink, results.map(_._3).toSeq)
    }.toSeq.sortBy(_.name))
  }

  def downloadSources(name: String, ver: String): Future[Int] = {
    logger.info(s"downloading package $name")

    val packageURL =
      s"https://hackage.haskell.org/package/$name-$ver/$name-$ver.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver / ver

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    destination.mkdirs()

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    try {
      downloadFile(packageURL, archive)
      logger.info(s"downloaded")

      archiver.extract(archive, destination)
      logger.info("extacted")

      val future = indexAPI.insertOrUpdate(name, ver)
      logger.info("DB updated")

      future
    } catch {
      case e: Exception =>
        Future[Int] {
          logger.info(e.getMessage)
          0
        }
    }
  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
    s"curl -o ${dstFile.getPath} $srcURL" !!
  }

}
