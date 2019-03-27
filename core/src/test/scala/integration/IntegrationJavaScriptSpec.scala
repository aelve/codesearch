package integration

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{JavaScriptSearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import integration.fakes.FakeDownloader
import org.scalatest.{FreeSpec, Matchers}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import slick.jdbc.PostgresProfile.api._

class IntegrationJavaScriptSpec extends FreeSpec with Matchers with ForAllTestContainer {

  override val container = PostgreSQLContainer()

  "Integration JavaScript Spec" in new TestFixture {

    val searchResult: Search.CSearchPage = httpClient.use { implicit backend =>
      val database = Database.forURL(
        driver = "org.postgresql.Driver",
        url = s"${container.jdbcUrl}?user=${container.username}&password=${container.password}"
      )
      implicit val downloader: Downloader[IO] = Downloader.create[IO]
      val npmDownloader: FakeDownloader[IO]   = FakeDownloader[IO](getMetaData("integration/meta/node.json"))
      val unarchiver                          = Unarchiver[IO]
      val nodeIndex                           = JavaScriptIndex(config, database)
      val nodeSearch                          = new JavaScriptSearch

      for {
        hackageMeta <- NpmMetaDownloader(config.languagesConfig.javascript, npmDownloader)
        _           <- nodeIndex.initDB
        _           <- hackageMeta.downloadMeta
        _           <- nodeIndex.updatePackages(Some(15))
        _           <- nodeIndex.buildIndex
        result <- nodeSearch.search(
          SearchRequest(
            lang = "javascript",
            query = "* > dom",
            filter = None,
            filePath = None,
            insensitive = true,
            spaceInsensitive = true,
            preciseMatch = true,
            sourcesOnly = true,
            page = 1
          )
        )
      } yield result
    }.unsafeRunSync()

    searchResult.total shouldBe 2
    searchResult.data shouldBe Seq(
      PackageResult(
        Package("000-webpack-1.0.0", "https://www.npmjs.com/package/000-webpack/v/1.0.0"),
        Seq(
          CodeSnippet(
            "www/public/js/index.js",
            "npm/000-webpack/1.0.0/www/public/js/index.js",
            1882,
            NonEmptyVector.of(1886, 1888),
            Seq(
              "/**",
              " * DOMProperty exports lookup objects that can be used like functions:",
              " *",
              " *   > DOMProperty.isValid['id']",
              " *   true",
              " *   > DOMProperty.isValid['foobar']",
              " *   undefined",
              " *",
              " * Although this may be confusing, it performs better in general.",
              " *",
              " * @see http://jsperf.com/key-exists"
            )
          )
        )
      )
    )
  }
}
