package codesearch.core

import java.nio.ByteBuffer
import java.util.concurrent.Executors

import cats.effect.IO
import codesearch.core.index._
import codesearch.core.db._
import codesearch.core.model._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import fs2.Stream
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object Main {

  private val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  private implicit val ec: ExecutionContext = ExecutionContext
    .fromExecutorService(Executors.newFixedThreadPool(2 * Runtime.getRuntime.availableProcessors()))
  private implicit val fs2HttpClient: SttpBackend[IO, Stream[IO, ByteBuffer]] = AsyncHttpClientFs2Backend[IO]()

  case class Config(
      updatePackages: Boolean = false,
      downloadMeta: Boolean = false,
      initDB: Boolean = false,
      buildIndex: Boolean = false,
      lang: String = "all"
  )

  private val parser = new scopt.OptionParser[Config]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[Unit]('u', "update-packages") action { (_, c) =>
      c.copy(updatePackages = true)
    } text "update package-sources"

    opt[Unit]('d', "download-meta") action { (_, c) =>
      c.copy(downloadMeta = true)
    } text "update package meta information"

    opt[Unit]('i', "init-database") action { (_, c) =>
      c.copy(initDB = true)
    } text "create tables for database"

    opt[Unit]('b', "build-index") action { (_, c) =>
      c.copy(buildIndex = true)
    } text "build index with only latest packages"

    opt[String]('l', "lang") action { (l, c) =>
      c.copy(lang = l)
    }
  }

  case class LangRep[T <: DefaultTable](db: DefaultDB[T], langIndex: LanguageIndex[T])

  private val langReps = Map(
    "haskell"    -> LangRep[HackageTable](HackageDB, HaskellIndex()),
    "rust"       -> LangRep[CratesTable](CratesDB, RustIndex()),
    "ruby"       -> LangRep[GemTable](GemDB, RubyIndex()),
    "javascript" -> LangRep[NpmTable](NpmDB, JavaScriptIndex())
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

      if (c.downloadMeta) {
        c.lang match {
          case "all" =>
            langReps.values.foreach(_.langIndex.downloadMetaInformation())
          case lang =>
            langReps(lang).langIndex.downloadMetaInformation()
        }
      }

      if (c.updatePackages) {
        val future = c.lang match {
          case "all" =>
            Future
              .sequence(langReps.values.map(_.langIndex.updatePackages()))
              .map(_.sum)
          case lang =>
            langReps(lang).langIndex.updatePackages()
        }
        val cntUpdated = Await.result(future, Duration.Inf)

        logger.info(s"updated: $cntUpdated")
      }

      if (c.buildIndex) {
        val future = c.lang match {
          case "all" =>
            Future.sequence(langReps.values.map(_.langIndex.buildIndex()))
          case lang =>
            langReps(lang).langIndex.buildIndex()
        }
        Await.ready(future, Duration.Inf)
        logger.info(s"${c.lang} packages successfully indexed")
      }
    }
    fs2HttpClient.close()
    scala.sys.exit(0)
  }
}
