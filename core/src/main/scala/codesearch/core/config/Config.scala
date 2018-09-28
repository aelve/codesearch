package codesearch.core.config

import pureconfig.error.ConfigReaderFailures
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint, loadConfig}

case class Config(mydb: DatabaseConfig, snippetConfig: SnippetConfig)
case class DatabaseConfig(dataSourceClass: String, properties: DatabaseDetails)
case class DatabaseDetails(portNumber: Int, databaseName: String, user: String, password: String)
case class SnippetConfig(pageSize: Int, linesBefore: Int, linesAfter: Int)

object Config {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load(): Either[ConfigReaderFailures, Config] = loadConfig[Config]

}
