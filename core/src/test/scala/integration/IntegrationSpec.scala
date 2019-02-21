package integration

import java.nio.file.Paths
import java.sql.DriverManager

import cats.effect.{ContextShift, IO, Resource}
import cats.syntax.flatMap._
import fs2.Stream
import fs2.io.file
import codesearch.core._
import codesearch.core.Main._
import codesearch.core.config.Config
import codesearch.core.db._
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.model._
import codesearch.core.util.Unarchiver
import com.dimafeng.testcontainers.{ForEachTestContainer, PostgreSQLContainer}
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import fakes.FakeDownloader
import org.scalatest.{FreeSpec, Matchers}

class IntegrationSpec extends FreeSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(BlockingEC)

  def getMetaData(path: String): Stream[IO, Byte] = {
    val resource = Paths.get(getClass.getClassLoader.getResource(path).toURI)
    file.readAll[IO](resource, BlockingEC, 4096)
  }

  val hackageDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/haskell.tar.gz"))
  val nodeDownloader: FakeDownloader[IO]    = FakeDownloader[IO](getMetaData("integration/meta/node.json"))
  val rubyDownloader: FakeDownloader[IO]    = FakeDownloader[IO](getMetaData("integration/meta/ruby.gz"))
  val rustDownloader: FakeDownloader[IO]    = FakeDownloader[IO](getMetaData("integration/meta/rust.zip"))

  val params = Params(lang = "haskell", downloadMeta = true)

  Resource
    .make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close()))
    .use { implicit httpClient =>
      implicit val downloader: Downloader[IO] = Downloader.create[IO]

      for {
        config <- Config.load[IO]

        unarchiver = Unarchiver[IO]

        hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, hackageDownloader)
        cratesMeta  <- CratesMetaDownloader(config.languagesConfig.rust, unarchiver, rustDownloader)
        gemMeta     <- GemMetaDownloader(config.languagesConfig.ruby, rubyDownloader)
        npmMeta     <- NpmMetaDownloader(config.languagesConfig.javascript, nodeDownloader)

        langReps = Map(
          "haskell"    -> LangRep[HackageTable](HackageDB, HaskellIndex(config), hackageMeta),
          "rust"       -> LangRep[CratesTable](CratesDB, RustIndex(config), cratesMeta),
          "ruby"       -> LangRep[GemTable](GemDB, RubyIndex(config), gemMeta),
          "javascript" -> LangRep[NpmTable](NpmDB, JavaScriptIndex(config), npmMeta)
        )
        exitCode <- Program(langReps) >>= (_.run(params))
      } yield exitCode
    }
    .unsafeRunSync()
}
