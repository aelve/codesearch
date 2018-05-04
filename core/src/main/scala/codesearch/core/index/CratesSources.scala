package codesearch.core.index

import java.io.IOException

import ammonite.ops.{Path, pwd}
import codesearch.core.model.CratesTable
import codesearch.core.util.Helper
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CratesSources extends Sources[CratesTable] {
  val SOURCES: Path = pwd / 'data / 'rust / 'packages

  private val logger: Logger = LoggerFactory.getLogger(CratesSources.getClass)
  override val indexAPI: CratesIndex.type = CratesIndex

  def csearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean): Seq[PackageResult] = {
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
      args.append("-f", ".*\\.(rs)$")
    }
    args.append(query)

    val answer = (args #| Seq("head", "-1000")) .!!

    answer.split('\n').flatMap(CratesIndex.contentByURI).groupBy(_._1).map {
      case (verName, results) =>
        PackageResult(verName, results.map(_._2).toSeq)
    }.toSeq
  }


  def downloadSources(name: String, ver: String): Future[Int] = {
    SOURCES.toIO.mkdirs()

    try {
      s"rm -rf ${SOURCES / name}" !!

      s"cargo clone $name --vers $ver --prefix $SOURCES" !!

      logger.info("package cloned")

      val future = indexAPI.insertOrUpdate(name, ver)
      logger.info("DB updated")
      future
    } catch {
      case e: IOException =>
        Future[Int] {
          logger.info(e.getMessage)
          0
        }
    }
  }
}
