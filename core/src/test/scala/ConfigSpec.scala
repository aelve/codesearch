import codesearch.core.config.DatabaseConfig
import org.scalatest.{FlatSpec, Matchers}

class ConfigSpec extends FlatSpec with Matchers {
  val config = DatabaseConfig.load()

  "Config" should "be not null" in {
    config.right.get should not be null
  }
}
