package codesearch.core

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap._
import codesearch.core.config.Config
import codesearch.core.db._
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.model._
import codesearch.core.util.Unarchiver
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object Main extends IOApp {

  final case class Params(
      updatePackages: Boolean = false,
      downloadMeta: Boolean = false,
      buildIndex: Boolean = false,
      limitedCountPackages: Option[Int] = None,
      lang: String = "all"
  )

  case class LangRep[A <: DefaultTable](
      db: DefaultDB[A],
      langIndex: LanguageIndex[A],
      metaDownloader: MetaDownloader[IO]
  )

  def run(args: List[String]): IO[ExitCode] =
    Resource.make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close())).use { implicit httpClient =>
      for {
        params <- CLI.params(args)
        config <- Config.load[IO]

        unarchiver                            = Unarchiver[IO]
        implicit0(downloader: Downloader[IO]) = Downloader.create[IO]

        hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, downloader)
        cratesMeta  <- CratesMetaDownloader(config.languagesConfig.rust, unarchiver, downloader)
        gemMeta     <- GemMetaDownloader(config.languagesConfig.ruby, downloader)
        npmMeta     <- NpmMetaDownloader(config.languagesConfig.javascript, downloader)

        langReps = Map(
          "haskell"    -> LangRep[HackageTable](HackageDB, HaskellIndex(config), hackageMeta),
          "rust"       -> LangRep[CratesTable](CratesDB, RustIndex(config), cratesMeta),
          "ruby"       -> LangRep[GemTable](GemDB, RubyIndex(config), gemMeta),
          "javascript" -> LangRep[NpmTable](NpmDB, JavaScriptIndex(config), npmMeta)
        )
        exitCode <- Program(langReps) >>= (_.run(params))
      } yield exitCode
    }
}
