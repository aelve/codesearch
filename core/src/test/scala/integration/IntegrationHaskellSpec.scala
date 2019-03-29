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
import org.scalatest.FreeSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}

class IntegrationHaskellSpec extends FreeSpec with ForAllTestContainer with IntegrationSpecBase {

  override val container = PostgreSQLContainer()
  val searcher: Search   = new HaskellSearch

  "Integration Haskell Spec" in new TestFixture {

    httpClient.use { implicit backend =>
      implicit val downloader: Downloader[IO]   = Downloader.create[IO]
      val hackageDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/haskell.tar.gz"))
      val unarchiver                            = Unarchiver[IO]
      val haskellIndex                          = HaskellIndex(config, database)

      for {
        hackageMeta <- HackageMetaDownloader(config.languagesConfig.haskell, unarchiver, hackageDownloader)
        _           <- haskellIndex.initDB
        _           <- hackageMeta.downloadMeta
        _           <- haskellIndex.updatePackages(Some(20))
        _           <- haskellIndex.buildIndex
      } yield ()
    }.unsafeRunSync()

    searchResultsMustBe(
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
      ),
      1,
      Seq(
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
    )
  }
}
