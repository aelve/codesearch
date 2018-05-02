package codesearch.core

import ammonite.ops.{FilePath, pwd}
import codesearch.core.db.HackageDB
import codesearch.core.utilities.VersionsUtility
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
        val future = HackageDB.initDB()
        Await.result(future, Duration.Inf)
      }

      if (c.downloadIndex) {
        VersionsUtility.updateIndex()
      }

      if (c.updatePackages) {
        utilities.SourcesUtility.update()
      }
    }
  }
}
