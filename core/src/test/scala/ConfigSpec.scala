import codesearch.core.config.{Config, DatabaseDetails}
import org.scalatest.{FlatSpec, Matchers}

class ConfigSpec extends FlatSpec with Matchers {
  val config = Config.load()

  "Config" should "be not empty" in {
    config.right.get.mydb.dataSourceClass shouldBe "org.postgresql.ds.PGSimpleDataSource"
    config.right.get.mydb.properties shouldBe DatabaseDetails(5432, "sourcesdb", "postgres", "postgres")
  }
}
