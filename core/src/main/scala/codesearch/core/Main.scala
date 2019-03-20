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
import slick.jdbc.PostgresProfile.api._

object Main extends IOApp {

  final case class Params(
      updatePackages: Boolean = false,
      downloadMeta: Boolean = false,
      initDB: Boolean = false,
      buildIndex: Boolean = false,
      limitedCountPackages: Option[Int] = None,
      lang: String = "all"
  )

  case class LangRep[A <: DefaultTable](
      langIndex: LanguageIndex[A],
      metaDownloader: MetaDownloader[IO]
  )

  def run(args: List[String]): IO[ExitCode] =
    Resource.make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close())).use { implicit httpClient =>
      for {
        params <- CLI.params(args)
        config <- Config.load[IO]

        dbConfig = config.db

        val db = Database.forURL(
          driver = "org.postgresql.Driver",
          url =
            s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}?user=${dbConfig.user}&password=${dbConfig.password}"
        )

        unarchiver                            = Unarchiver[IO]
        implicit0(downloader: Downloader[IO]) = Downloader.create[IO]

        hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, downloader)
        cratesMeta  <- CratesMetaDownloader(config.languagesConfig.rust, unarchiver, downloader)
        gemMeta     <- GemMetaDownloader(config.languagesConfig.ruby, downloader)
        npmMeta     <- NpmMetaDownloader(config.languagesConfig.javascript, downloader)

        langReps = Map(
          "haskell"    -> LangRep[HackageTable](HaskellIndex(config, db), hackageMeta),
          "rust"       -> LangRep[CratesTable](RustIndex(config, db), cratesMeta),
          "ruby"       -> LangRep[GemTable](RubyIndex(config, db), gemMeta),
          "javascript" -> LangRep[NpmTable](JavaScriptIndex(config, db), npmMeta)
        )
        exitCode <- Program(langReps) >>= (_.run(params))
      } yield exitCode
    }
}
