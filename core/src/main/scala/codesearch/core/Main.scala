package codesearch.core

import ammonite.ops.{FilePath, pwd}
import codesearch.core.index._
import codesearch.core.db._
import codesearch.core.model._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  private val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  case class Config(updatePackages: Boolean = false,
                    downloadIndex: Boolean = false,
                    sourcesDir: FilePath = pwd / 'sources,
                    initDB: Boolean = false,
                    lang: String = "all"
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

    opt[String]('l', "lang") action { (l, c) =>
      c.copy(lang = l)
    }
  }

  case class LangRep[T <: DefaultTable](db: DefaultDB[T],
                                        index: Index,
                                        sources: Sources[T]
                                       )

  private val langReps = Map(
    "hackage" -> LangRep[HackageTable](HackageDB, HackageIndex, HackageSources),
    "crates"  -> LangRep[CratesTable](CratesDB, CratesIndex, CratesSources),
    "gem"     -> LangRep[GemTable](GemDB, GemIndex, GemSources),
    "npm"     -> LangRep[NpmTable](NpmDB, NpmIndex, NpmSources)
  )

  def main(args: Array[String]): Unit = {

    parser.parse(args, Config()) foreach { c =>

      if (c.lang != "all" && !(langReps.keySet contains c.lang)) {
        throw new IllegalArgumentException(s"Unsupported lanuages\n Available languages: ${langReps.keys}")
      }

      if (c.lang == "all") {
        logger.info("Codesearch-core started for all supported languages")
      } else {
        logger.info(s"Codesearch-core started for language ${c.lang}")
      }

      if (c.initDB) {
        val future = c.lang match {
          case "all" =>
            Future.sequence(langReps.values.map(_.db.initDB()))
          case lang =>
            langReps(lang).db.initDB()
        }
        Await.result(future, Duration.Inf)
      }

      if (c.downloadIndex) { c.lang match {
        case "all" =>
          langReps.values.foreach(_.index.updateIndex())
        case lang =>
          langReps(lang).index.updateIndex()
      } }

      if (c.updatePackages) {
        val future = c.lang match {
          case "all" =>
            Future
              .sequence(langReps.values.map(_.sources.update()))
              .map(_.sum)
          case lang =>
            langReps(lang).sources.update()
        }
        val cntUpdated = Await.result(future, Duration.Inf)

        logger.info(s"updated: $cntUpdated")
      }
    }

    scala.sys.exit(0)
  }
}
