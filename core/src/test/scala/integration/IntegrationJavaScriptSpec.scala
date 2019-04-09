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
import org.scalatest.FreeSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}

class IntegrationJavaScriptSpec extends FreeSpec with ForAllTestContainer with IntegrationSpecBase {

  override val container = PostgreSQLContainer()
  val searcher: Search   = new JavaScriptSearch

  "Integration JavaScript Spec" in new TestFixture {

    httpClient.use { implicit backend =>
      implicit val downloader: Downloader[IO] = Downloader.create[IO]
      val npmDownloader: FakeDownloader[IO]   = FakeDownloader[IO](getMetaData("integration/meta/node.json"))
      val unarchiver                          = Unarchiver[IO]
      val nodeIndex                           = JavaScriptIndex(config, database)

      for {
        hackageMeta <- NpmMetaDownloader(config.languagesConfig.javascript, npmDownloader)
        _           <- nodeIndex.initDB
        _           <- hackageMeta.downloadMeta
        _           <- nodeIndex.updatePackages(Some(15))
        _           <- nodeIndex.buildIndex
      } yield ()
    }.unsafeRunSync()

    searchResultsMustBe(
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
      ),
      2,
      Seq(
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
    )
    println("Test")
    searchResultsMustBe(
      SearchRequest(
        lang = "javascript",
        query = "import styled",
        filter = Some("css"),
        filePath = None,
        insensitive = false,
        spaceInsensitive = false,
        preciseMatch = true,
        sourcesOnly = true,
        page = 1
      ),
      1,
      Seq(
        PackageResult(
          Package("00-components-0.5.0", "https://www.npmjs.com/package/00-components/v/0.5.0"),
          Seq(
            CodeSnippet(
              "dist/Button.js",
              "npm/00-components/0.5.0/dist/Button.js",
              19,
              NonEmptyVector.of(23),
              Seq(
                "  return data;",
                "}",
                "",
                "import styled, { css } from \"styled-components\";",
                "import defaultTheme from \"./theme/default\";",
                "var Button = styled.a(_templateObject(), function (_ref) {",
                "  var theme = _ref.theme;",
                "  return theme.fontFamily;",
                "}, function (props) {"
              )
            )
          )
        )
      )
    )
  }
}
