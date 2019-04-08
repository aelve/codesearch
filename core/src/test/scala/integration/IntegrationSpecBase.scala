package integration

import codesearch.core.search.Search.PackageResult
import codesearch.core.search.{Search, SearchRequest}
import slick.jdbc.PostgresProfile.api._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.Matchers

trait IntegrationSpecBase extends Matchers {

  val container: PostgreSQLContainer
  val searcher: Search

  def database = Database.forURL(
    driver = "org.postgresql.Driver",
    url = s"${container.jdbcUrl}?user=${container.username}&password=${container.password}"
  )

  def searchResultsMustBe(lookFor: SearchRequest, totalResults: Int, result: Seq[PackageResult]): Unit = {
    val searchResult = searcher
      .search(lookFor)
      .unsafeRunSync()

    searchResult.total shouldBe totalResults
    searchResult.data shouldBe result
    println("Hello")
  }
}
