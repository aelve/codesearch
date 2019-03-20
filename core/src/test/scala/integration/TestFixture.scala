package integration

import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Paths

import cats.effect.{IO, Resource}
import codesearch.core.BlockingEC
import codesearch.core.config._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import fs2.Stream
import fs2.io.file

import scala.concurrent.ExecutionContext.Implicits.global

trait TestFixture {

  implicit val cs = IO.contextShift(global)

  def httpClient: Resource[IO, SttpBackend[IO, Stream[IO, ByteBuffer]]] =
    Resource.make(IO(AsyncHttpClientFs2Backend[IO]()))(client => IO(client.close()))

  def getMetaData(path: String): Stream[IO, Byte] = {
    val resource = Paths.get(getClass.getClassLoader.getResource(path).toURI)
    file.readAll[IO](resource, BlockingEC, 4096)
  }

  def config = Config(
    DatabaseConfig(
      "slick.jdbc.DatabaseUrlDataSource",
      "localhost",
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
        Paths.get("./data/test/meta/haskell/index.tar.gz"),
        Paths.get("./data/test/meta/haskell/"),
        30
      ),
      RubyConfig(
        new URI("http://rubygems.org/latest_specs.4.8.gz"),
        Paths.get("./data/test/meta/ruby/ruby_index.gz"),
        Paths.get("./data/test/meta/ruby/ruby_index.json"),
        Paths.get("./scripts/update_index.rb"),
        30
      ),
      RustConfig(
        new URI("https://github.com/rust-lang/crates.io-index/archive/master.zip"),
        Paths.get("./data/test/meta/rust/archive.zip"),
        Paths.get("./data/test/meta/rust/"),
        30
      ),
      JavaScriptConfig(
        new URI("https://replicate.npmjs.com/_all_docs?include_docs=true"),
        Paths.get("./data/test/meta/npm/npm_packages_index.json"),
        30
      )
    )
  )
}