package integration

import java.nio.file.Paths

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.directory.HaskellCindex
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{HaskellSearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import integration.fakes.FakeDownloader
import org.scalatest.FreeSpec

class IntegrationHaskellSpec extends FreeSpec with ForAllTestContainer with IntegrationSpecBase {

  override val container = PostgreSQLContainer()
  val haskellCindex      = HaskellCindex(Paths.get("./index/test/cindex/"))
  val searcher: Search   = new HaskellSearch(haskellCindex)

  "Integration Haskell Spec" in new TestFixture {

    httpClient.use { implicit backend =>
      implicit val downloader: Downloader[IO]   = Downloader.create[IO]
      val hackageDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/haskell.tar.gz"))
      val unarchiver                            = Unarchiver[IO]
      val haskellIndex                          = HaskellIndex(config, database, haskellCindex)

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
        page = 1,
        withoutTests = false
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

    searchResultsMustBe(
      SearchRequest(
        lang = "haskell",
        query = "#!/usr.*runghc",
        filter = None,
        filePath = Some(".*hs"),
        insensitive = false,
        spaceInsensitive = false,
        preciseMatch = false,
        sourcesOnly = true,
        page = 1,
        withoutTests = false
      ),
      1,
      Seq(
        PackageResult(
          Package("3d-graphics-examples-0.0.0.2", "https://hackage.haskell.org/package/3d-graphics-examples-0.0.0.2"),
          Seq(
            CodeSnippet(
              "Setup.lhs",
              "hackage/3d-graphics-examples/0.0.0.2/Setup.lhs",
              0,
              NonEmptyVector.of(1),
              Seq("#!/usr/bin/env runghc", "", "> import Distribution.Simple", "> main = defaultMain")
            )
          )
        )
      )
    )
  }
}
