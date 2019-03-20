package integration

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{HaskellSearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import integration.fakes.FakeDownloader
import org.scalatest.{FreeSpec, Matchers}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import slick.jdbc.PostgresProfile.api._

class IntegrationHaskellSpec extends FreeSpec with Matchers with ForAllTestContainer {

  override val container = PostgreSQLContainer()

  "Integration Haskell Spec" in new TestFixture {

    val searchResult: Search.CSearchPage = httpClient.use { implicit backend =>
      val database = Database.forURL(
        driver = "org.postgresql.Driver",
        url = s"${container.jdbcUrl}?user=${container.username}&password=${container.password}"
      )
      implicit val downloader: Downloader[IO]   = Downloader.create[IO]
      val hackageDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/haskell.tar.gz"))
      val unarchiver                            = Unarchiver[IO]
      val haskellIndex                          = HaskellIndex(config, database)
      val haskellSearch                         = new HaskellSearch

      for {
        hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, hackageDownloader)
        _           <- haskellIndex.initDB
        _           <- hackageMeta.downloadMeta
        _           <- haskellIndex.updatePackages(Some(20))
        _           <- haskellIndex.buildIndex
        result <- haskellSearch.search(
          SearchRequest(
            lang = "haskell",
            query = "Tupel",
            filter = None,
            filePath = None,
            insensitive = false,
            spaceInsensitive = false,
            preciseMatch = false,
            sourcesOnly = false,
            page = 1
          )
        )
      } yield result
    }.unsafeRunSync()

    searchResult.total shouldBe 1
    searchResult.data shouldBe Seq(
      PackageResult(
        Package("3d-graphics-examples-0.0.0.2", "https://hackage.haskell.org/package/3d-graphics-examples-0.0.0.2"),
        Seq(
          CodeSnippet(
            "src/mountains/Mountains.hs",
            "hackage/3d-graphics-examples/0.0.0.2/src/mountains/Mountains.hs",
            32,
            NonEmptyVector.of(36),
            Seq(
              "",
              "",
              "type Pair t = (t, t)",
              "type Tupel3 t = (t, t, t)",
              "",
              "data TerrainDrawMode = TerrainPoints | TerrainWireframe | TerrainSolid",
              "  deriving (Eq, Show)",
              "",
              "data Distribution = UniformDistribution | NormalDistribution"
            )
          )
        )
      )
    )
  }
}
