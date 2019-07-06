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
      dataSourceClass = "slick.jdbc.DatabaseUrlDataSource",
      host = "localhost",
      port = 5432,
      name = "sourcesdb",
      user = "postgres",
      password = "postgres"
    ),
    SnippetConfig(
      pageSize = 30,
      linesBefore = 3,
      linesAfter = 5
    ),
    LanguagesConfig(
      HaskellConfig(
        repoIndexUrl = new URI("http://hackage.haskell.org/packages/index.tar.gz"),
        repoArchivePath = Paths.get("./data/test/meta/haskell/index.tar.gz"),
        repoPath = Paths.get("./data/test/meta/haskell/"),
        concurrentTasksCount = 30
      ),
      RubyConfig(
        repoIndexUrl = new URI("http://rubygems.org/latest_specs.4.8.gz"),
        repoArchivePath = Paths.get("./data/test/meta/ruby/ruby_index.gz"),
        repoJsonPath = Paths.get("./data/test/meta/ruby/ruby_index.json"),
        scriptPath = Paths.get("./scripts/update_index.rb"),
        concurrentTasksCount = 30
      ),
      RustConfig(
        repoIndexUrl = new URI("https://github.com/rust-lang/crates.io-index/archive/master.zip"),
        repoArchivePath = Paths.get("./data/test/meta/rust/archive.zip"),
        repoPath = Paths.get("./data/test/meta/rust/"),
        concurrentTasksCount = 30
      ),
      JavaScriptConfig(
        repoIndexUrl = new URI("https://replicate.npmjs.com/_all_docs?include_docs=true"),
        repoJsonPath = Paths.get("./data/test/meta/npm/npm_packages_index.json"),
        concurrentTasksCount = 30
      )
    ),
    MetricsConfig(false)
  )
}
