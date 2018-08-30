package codesearch.core.config

import codesearch.core.config.DatabaseConfig.{DatabaseConfig, DatabaseDetails}
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}
import pureconfig.error.ConfigReaderFailures
import pureconfig.loadConfig

case class Config(mydb: DatabaseConfig)

object DatabaseConfig {

  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  case class DatabaseConfig(dataSourceClass: String, properties: DatabaseDetails)
  case class DatabaseDetails(portNumber: Int, databaseName: String, user: String, password: String,
                             dataSourceClass: String = "org.postgresql.ds.PGSimpleDataSource")

  def load(): Either[ConfigReaderFailures, Config] = loadConfig[Config]

  def empty(): DatabaseConfig =
    new DatabaseConfig(" ", DatabaseDetails(0, "", "", "", ""))
}
