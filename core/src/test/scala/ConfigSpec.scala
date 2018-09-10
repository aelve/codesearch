import codesearch.core.config.DatabaseConfig
import codesearch.core.config.DatabaseConfig.DatabaseDetails
import org.scalatest.{FlatSpec, Matchers}

class ConfigSpec extends FlatSpec with Matchers {
  val config = DatabaseConfig.load()

  "Config" should "be not empty" in {
    config.right.get.mydb.dataSourceClass shouldBe "org.postgresql.ds.PGSimpleDataSource"
    config.right.get.mydb.properties shouldBe DatabaseDetails(5432, "sourcesdb", "postgres", "postgres")
  }
}
