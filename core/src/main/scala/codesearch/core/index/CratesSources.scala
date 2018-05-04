package codesearch.core.index

import java.io.IOException

import ammonite.ops.pwd
import codesearch.core.model.CratesTable
import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CratesSources extends Sources[CratesTable] {
  private val logger: Logger = LoggerFactory.getLogger(CratesSources.getClass)
  override val indexAPI: CratesIndex.type = CratesIndex

  def downloadSources(name: String, ver: String): Future[Int] = {
    val SOURCES = pwd / 'data / 'rust / 'packages
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
