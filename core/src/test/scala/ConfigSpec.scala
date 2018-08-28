import codesearch.core.config.Config
import org.scalatest.{FlatSpec, Matchers}
import pureconfig.error.ConfigReaderFailures
import pureconfig.loadConfig

class ConfigSpec extends FlatSpec with Matchers {
  val config: Either[ConfigReaderFailures, Config] = loadConfig[Config]
  println(config.right.get)
  config.right should not be null
}
