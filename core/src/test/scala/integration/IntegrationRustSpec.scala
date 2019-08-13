package integration

import java.nio.file.Paths

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.directory.RustCindex
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{RustSearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import integration.fakes.FakeDownloader
import org.scalatest.FreeSpec

class IntegrationRustSpec extends FreeSpec with ForAllTestContainer with IntegrationSpecBase {

  override val container = PostgreSQLContainer()
  val rustCindex         = RustCindex(Paths.get("./index/test/cindex/"))
  val searcher: Search   = new RustSearch(rustCindex)

  "Integration Rust Spec" in new TestFixture {

    httpClient.use { implicit backend =>
      implicit val downloader: Downloader[IO]  = Downloader.create[IO]
      val cratesDownloader: FakeDownloader[IO] = FakeDownloader[IO](getMetaData("integration/meta/rust.zip"))
      val unarchiver                           = Unarchiver[IO]
      val rustIndex                            = RustIndex(config, database, rustCindex)

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
        filter = Some("ErrorKind"),
        filePath = Some("lapin-futures-tls-internal.*6"),
        insensitive = true,
        spaceInsensitive = false,
        preciseMatch = true,
        sourcesOnly = false,
        page = 1,
        withoutTests = false
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

    searchResultsMustBe(
      SearchRequest(
        lang = "rust",
        query = "Box::new(AMQP",
        filter = Some("AMQPFrame"),
        filePath = None,
        insensitive = true,
        spaceInsensitive = false,
        preciseMatch = true,
        sourcesOnly = true,
        page = 1,
        withoutTests = false
      ),
      1,
      Seq(
        PackageResult(
          Package("lapin-futures-0.17.0", "https://docs.rs/crate/lapin-futures/0.17.0"),
          Seq(
            CodeSnippet(
              "src/transport.rs",
              "crates/lapin-futures/0.17.0/src/transport.rs",
              337,
              NonEmptyVector.of(341),
              Seq(
                "",
                "    let mut codec = AMQPCodec { frame_max: 8192 };",
                "    let mut buffer = BytesMut::with_capacity(8192);",
                "    let frame = AMQPFrame::Header(0, 10, Box::new(AMQPContentHeader {",
                "      class_id: 10,",
                "      weight: 0,",
                "      body_size: 64,",
                "      properties: BasicProperties::default()",
                "    }));"
              )
            )
          )
        )
      )
    )
  }
}
