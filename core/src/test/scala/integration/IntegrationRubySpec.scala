package integration

import cats.data.NonEmptyVector
import cats.effect.IO
import codesearch.core.index._
import codesearch.core.index.repository.Downloader
import codesearch.core.meta._
import codesearch.core.search.Search.{CodeSnippet, Package, PackageResult}
import codesearch.core.search.{RubySearch, Search, SearchRequest}
import codesearch.core.util.Unarchiver
import integration.fakes.FakeDownloader
import org.scalatest.{FreeSpec, Matchers}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import slick.jdbc.PostgresProfile.api._

class IntegrationRubySpec extends FreeSpec with Matchers with ForAllTestContainer {

  override val container = PostgreSQLContainer()

  "Integration Ruby Spec" in new TestFixture {

    val searchResults: (Search.CSearchPage, Search.CSearchPage) = httpClient.use { implicit backend =>
      val database = Database.forURL(
        driver = "org.postgresql.Driver",
        url = s"${container.jdbcUrl}?user=${container.username}&password=${container.password}"
      )
      implicit val downloader: Downloader[IO] = Downloader.create[IO]
      val gemDownloader: FakeDownloader[IO]   = FakeDownloader[IO](getMetaData("integration/meta/ruby.gz"))
      val unarchiver                          = Unarchiver[IO]
      val rubyIndex                           = RubyIndex(config, database)
      val rubySearch                          = new RubySearch

      for {
        gemMeta <- GemMetaDownloader(config.languagesConfig.ruby, gemDownloader)
        _       <- rubyIndex.initDB
        _       <- gemMeta.downloadMeta
        _       <- rubyIndex.updatePackages(Some(14))
        _       <- rubyIndex.buildIndex
        firstResult <- rubySearch.search(
          SearchRequest(
            lang = "ruby",
            query = "class flex4sdk",
            filter = None,
            filePath = None,
            insensitive = true,
            spaceInsensitive = false,
            preciseMatch = false,
            sourcesOnly = false,
            page = 1
          )
        )
        secondResult <- rubySearch.search(
          SearchRequest(
            lang = "ruby",
            query = "fest",
            filter = None,
            filePath = None,
            insensitive = false,
            spaceInsensitive = false,
            preciseMatch = false,
            sourcesOnly = false,
            page = 1
          )
        )
      } yield (firstResult, secondResult)
    }.unsafeRunSync()

    val (firstResult, secondResult) = searchResults

    firstResult.total shouldBe 1
    firstResult.data shouldBe Seq(
      PackageResult(
        Package("sprout-flex4sdk-tool-4.2.14", "https://rubygems.org/gems/sprout-flex4sdk-tool/versions/4.2.14"),
        Seq(
          CodeSnippet(
            "lib/sprout/flex4sdk/version.rb",
            "gem/sprout-flex4sdk-tool/4.2.14/lib/sprout/flex4sdk/version.rb",
            0,
            NonEmptyVector.of(2),
            Seq(
              "module Sprout",
              "  class Flex4SDK #:nodoc:",
              "    module VERSION #:nodoc:",
              "      MAJOR = 4",
              "      MINOR = 2",
              "      TINY  = 14",
              ""
            )
          )
        )
      )
    )

    secondResult.total shouldBe 2
    secondResult.data shouldBe Seq(
      PackageResult(
        Package("sprout-flex4-bundle-0.1.4", "https://rubygems.org/gems/sprout-flex4-bundle/versions/0.1.4"),
        Seq(
          CodeSnippet(
            "lib/sprout/generators/component/component_generator.rb",
            "gem/sprout-flex4-bundle/0.1.4/lib/sprout/generators/component/component_generator.rb",
            0,
            NonEmptyVector.of(4),
            Seq(
              "",
              "class ComponentGenerator < Sprout::Generator::NamedBase  # :nodoc:",
              "",
              "  def manifest",
              "    record do |m|",
              "      if(!user_requested_test)",
              "        m.directory full_class_dir",
              "        m.template 'Component.mxml', full_class_path.gsub(/.as$/, '.mxml')",
              "      end"
            )
          ),
          CodeSnippet(
            "lib/sprout/generators/project/project_generator.rb",
            "gem/sprout-flex4-bundle/0.1.4/lib/sprout/generators/project/project_generator.rb",
            0,
            NonEmptyVector.of(4),
            Seq(
              "",
              "class ProjectGenerator < Sprout::Generator::NamedBase # :nodoc:",
              "",
              "  def manifest",
              "    record do |m|",
              "      base = class_name",
              "      m.directory base",
              "      m.directory File.join(base, 'assets/skins', project_name)",
              "      m.directory File.join(base, 'bin')"
            )
          )
        )
      )
    )
  }
}
