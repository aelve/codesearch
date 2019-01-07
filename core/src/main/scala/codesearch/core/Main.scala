package codesearch.core

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap._
import codesearch.core.config.Config
import codesearch.core.db._
import codesearch.core.index._
import codesearch.core.model._
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object Main extends IOApp {

  final case class Params(
      updatePackages: Boolean = false,
      downloadMeta: Boolean = false,
      initDB: Boolean = false,
      buildIndex: Boolean = false,
      lang: String = "all"
  )

  case class LangRep[A <: DefaultTable](db: DefaultDB[A], langIndex: LanguageIndex[A])

  def run(args: List[String]): IO[ExitCode] =
    Resource.make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close())).use { implicit httpClient =>
      for {
        params <- CLI.params(args)
        config <- Config.load
        langReps = Map(
          "haskell"    -> LangRep[HackageTable](HackageDB, HaskellIndex(config)),
          "rust"       -> LangRep[CratesTable](CratesDB, RustIndex(config)),
          "ruby"       -> LangRep[GemTable](GemDB, RubyIndex(config)),
          "javascript" -> LangRep[NpmTable](NpmDB, JavaScriptIndex(config))
        )
        exitCode <- Program(langReps) >>= (_.run(params))
      } yield exitCode
    }
}
