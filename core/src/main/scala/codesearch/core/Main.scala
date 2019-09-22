package codesearch.core

import java.nio.file.Paths

import cats.effect.internals.IOContextShift
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap._
import codesearch.core.config.Config
import codesearch.core.index._
import codesearch.core.index.directory._
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

  def run(args: List[String]): IO[ExitCode] = {
    Resource.make(IO(Database.forConfig("db")))(con => IOContextShift.global.evalOn(BlockingEC)(IO(con.close()))).use {
      db =>
        Resource.make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close())).use { implicit httpClient =>
          for {
            params <- CLI.params(args)
            config <- Config.load[IO]

            unarchiver                            = Unarchiver[IO]
            implicit0(downloader: Downloader[IO]) = Downloader.create[IO]

            hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, downloader)
            cratesMeta  <- CratesMetaDownloader(config.languagesConfig.rust, unarchiver, downloader)

            cindexPath = Paths.get("./index/cindex/")
            haskellCindex = HaskellCindex(cindexPath)
            rustCindex    = RustCindex(cindexPath)

            langReps = Map(
              "haskell" -> LangRep[HackageTable](HaskellIndex(config, db, haskellCindex), hackageMeta),
              "rust"    -> LangRep[CratesTable](RustIndex(config, db, rustCindex), cratesMeta)
            )
            exitCode <- Program(langReps) >>= (_.run(params))
          } yield exitCode
        }
    }
  }
}
