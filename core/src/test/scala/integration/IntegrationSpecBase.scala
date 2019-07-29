package integration

import codesearch.core.search.Search.{PackageResult, SuccessResponse}
import codesearch.core.search.{Search, SearchRequest}
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.Matchers
import slick.jdbc.PostgresProfile.api._

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

    searchResult match {
      case x: SuccessResponse => {
        x.totalMatches shouldBe totalResults
        x.packages shouldBe result
      }
    }
  }
}
