package codesearch.core

import ammonite.ops.{FilePath, pwd}
import codesearch.core.index.{CratesIndex, CratesSources, HackageIndex, HackageSources}
import codesearch.core.db.{CratesDB, HackageDB}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  private val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  case class Config(updatePackages: Boolean = false,
                    downloadIndex: Boolean = false,
                    sourcesDir: FilePath = pwd / 'sources,
                    initDB: Boolean = false
                   )

  private val parser = new scopt.OptionParser[Config]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[Unit]('u', "update-packages") action { (_, c) =>
      c.copy(updatePackages = true)
    } text "update package-sources"

    opt[Unit]('d', "download-index") action { (_, c) =>
      c.copy(downloadIndex = true)
    } text "update package-index"

    opt[Unit]('i', "init-database") action { (_, c) =>
      c.copy(initDB = true)
    } text "create tables for database"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Codesearch-core started")

    parser.parse(args, Config()) foreach { c =>
      if (c.initDB) {
        val future = Future.sequence(Seq(
          HackageDB.initDB(), CratesDB.initDB()))
        Await.result(future, Duration.Inf)
      }

      if (c.downloadIndex) {
        HackageIndex.updateIndex()
        CratesIndex.updateIndex()
      }

      if (c.updatePackages) {
        val future = Future.sequence(Seq(
          CratesSources.update(), HackageSources.update()))
            .map(_.sum)
        val cntUpdated = Await.result(future, Duration.Inf)

        logger.info(s"updated: $cntUpdated")
      }
    }
  }
}
