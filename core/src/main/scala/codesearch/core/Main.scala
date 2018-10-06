package codesearch.core

import java.nio.ByteBuffer
import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import codesearch.core.config.Config
import codesearch.core.db._
import codesearch.core.index._
import codesearch.core.model._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  private implicit val ec: ExecutionContext = ExecutionContext
    .fromExecutorService(Executors.newFixedThreadPool(2 * Runtime.getRuntime.availableProcessors()))
  private implicit val fs2HttpClient: SttpBackend[IO, Stream[IO, ByteBuffer]] = AsyncHttpClientFs2Backend[IO]()

  case class Params(
      updatePackages: Boolean = false,
      downloadMeta: Boolean = false,
      initDB: Boolean = false,
      buildIndex: Boolean = false,
      lang: String = "all"
  )

  case class LangRep[A <: DefaultTable](db: DefaultDB[A], langIndex: LanguageIndex[A])

  def run(args: List[String]): IO[ExitCode] =
    for {
      logger <- Slf4jLogger.fromClass[IO](getClass)
      params <- CLI.params(args)

      config <- Config.load

      langReps = Map(
        "haskell"    -> LangRep[HackageTable](HackageDB, HaskellIndex(config)),
        "rust"       -> LangRep[CratesTable](CratesDB, RustIndex(config)),
        "ruby"       -> LangRep[GemTable](GemDB, RubyIndex(config)),
        "javascript" -> LangRep[NpmTable](NpmDB, JavaScriptIndex(config))
      )

      exitCode <- new Program(langReps, logger, fs2HttpClient, ec).run(params)
    } yield exitCode
}
