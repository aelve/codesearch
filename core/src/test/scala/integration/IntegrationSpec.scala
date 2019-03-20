/*package integration

import java.nio.file.Paths
import java.sql.DriverManager

import cats.effect.{ContextShift, IO, Resource}
import cats.syntax.flatMap._
import fs2.Stream
import fs2.io.file
import codesearch.core._
import codesearch.core.Main._
import codesearch.core.db._
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.config._
import codesearch.core.model._
import codesearch.core.util.Unarchiver
import com.dimafeng.testcontainers.{ForEachTestContainer, PostgreSQLContainer}
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import fakes.FakeDownloader
import org.scalatest.{FreeSpec, Matchers}
import java.net.URI


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

  val config = Config(
    DatabaseConfig(
      "slick.jdbc.DatabaseUrlDataSource",
      5432,
      "sourcesdb",
      "postgres",
      "postgres",
    ),
    SnippetConfig(
      1,
      1,
      1
    ),
    LanguagesConfig(
      HaskellConfig(
        new URI("http://hackage.haskell.org/packages/index.tar.gz"),
        Paths.get("./data/meta/haskell/index.tar.gz"),
        Paths.get("./data/meta/haskell/"),
        30
      ),
      RubyConfig(
        new URI("http://rubygems.org/latest_specs.4.8.gz"),
        Paths.get("./data/meta/ruby/ruby_index.gz"),
        Paths.get("./data/meta/ruby/ruby_index.json"),
        Paths.get("./scripts/update_index.rb"),
        30
      ),
      RustConfig(
        new URI("https://github.com/rust-lang/crates.io-index/archive/master.zip"),
        Paths.get("./data/meta/rust/archive.zip"),
        Paths.get("./data/meta/rust/"),
        30
      ),
      JavaScriptConfig(
        new URI("https://replicate.npmjs.com/_all_docs?include_docs=true"),
        Paths.get("./data/meta/npm/npm_packages_index.json"),
        30
      )
    ),
  )

  Resource
    .make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close()))
    .use { implicit httpClient =>
      implicit val downloader: Downloader[IO] = Downloader.create[IO]

      for {
        _ <- Config.load[IO]

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

        _ <- langReps("haskell").db.initDB
        _ <- langReps("rust").db.initDB
        _ <- langReps("ruby").db.initDB
        _ <- langReps("javascript").db.initDB

        exitCode <- Program(langReps) >>= (_.run(params))
      } yield exitCode
    }
    .unsafeRunSync()
}
*/