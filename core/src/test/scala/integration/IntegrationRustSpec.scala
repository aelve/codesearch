package integration

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{RustSearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import integration.fakes.FakeDownloader
import org.scalatest.FreeSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}

class IntegrationRustSpec extends FreeSpec with ForAllTestContainer with IntegrationSpecBase {

  override val container = PostgreSQLContainer()
  val searcher: Search   = new RustSearch

  "Integration Rust Spec" in new TestFixture {

    httpClient.use { implicit backend =>
      implicit val downloader: Downloader[IO]  = Downloader.create[IO]
      val cratesDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/rust.zip"))
      val unarchiver                           = Unarchiver[IO]
      val rustIndex                            = RustIndex(config, database)

      for {
        hackageMeta <- CratesMetaDownloader(config.languagesConfig.rust, unarchiver, cratesDownloader)
        _           <- rustIndex.initDB
        _           <- hackageMeta.downloadMeta
        _           <- rustIndex.updatePackages(Some(15))
        _           <- rustIndex.buildIndex
      } yield ()
    }.unsafeRunSync()

    searchResultsMustBe(
      SearchRequest(
        lang = "rust",
        query = "#[derive",
        filter = Some("errorKind"),
        filePath = Some("lapin-futures-tls-internal.*6"),
        insensitive = true,
        spaceInsensitive = false,
        preciseMatch = true,
        sourcesOnly = false,
        page = 1
      ),
      2,
      Seq(
        PackageResult(
          Package("lapin-futures-tls-internal-0.6.0", "https://docs.rs/crate/lapin-futures-tls-internal/0.6.0"),
          Seq(
            CodeSnippet(
              "src/error.rs",
              "crates/lapin-futures-tls-internal/0.6.0/src/error.rs",
              10,
              NonEmptyVector.of(14),
              Seq(
                "/// means that this type guaranteed to be both sendable and usable across",
                "/// threads, and that you'll be able to use the downcasting feature of the",
                "/// `failure::Error` type.",
                "#[derive(Debug)]",
                "pub struct Error {",
                "    inner: Context<ErrorKind>,",
                "}",
                "",
                "/// The different kinds of errors that can be reported."
              )
            ),
            CodeSnippet(
              "src/error.rs",
              "crates/lapin-futures-tls-internal/0.6.0/src/error.rs",
              19,
              NonEmptyVector.of(23),
              Seq(
                "///",
                "/// Even though we expose the complete enumeration of possible error variants, it is not",
                "/// considered stable to exhaustively match on this enumeration: do it at your own risk.",
                "#[derive(Debug, Fail)]",
                "pub enum ErrorKind {",
                "    /// Failure to parse an Uri",
                "    #[fail(display = \"Uri parsing error: {:?}\", _0)]",
                "    UriParsingError(String),",
                "    /// Failure to resolve a domain name"
              )
            )
          )
        )
      )
    )
  }
}
