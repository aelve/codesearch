package codesearch.core.config

import codesearch.core.config.Config.DatabaseConfig
import pureconfig.error.ConfigReaderFailures
import pureconfig.loadConfig

final case class Config(dataSourceClass: String, numThreads:Int, properties: DatabaseConfig)

object Config {
  private[config] final case class DatabaseConfig(portNumber: Int, name: String, user: String, password: String)

  def load(): Either[ConfigReaderFailures, Config] = loadConfig[Config]

  implicit class ConfigReaderFailuresExt(val value: ConfigReaderFailures) extends AnyVal {
    def printError(): String = value.toList.foldLeft("")((acc, e) => acc + e.description + "\n")
  }

  def empty(): Config =
    new Config("",0, DatabaseConfig(0, "", "", ""))
}

