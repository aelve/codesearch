package codesearch.core.config

import java.net.URI

import cats.effect.Sync
import pureconfig.module.catseffect._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

case class Config(
    db: DatabaseConfig,
    snippetConfig: SnippetConfig,
    languagesConfig: LanguagesConfig,
    metrics: MetricsConfig
)

case class DatabaseConfig(
    dataSourceClass: String,
    port: Int,
    name: String,
    user: String,
    password: String,
    properties: DatabaseProperties
)

case class DatabaseProperties(
    driver: String,
    url: String
)

case class SnippetConfig(
    pageSize: Int,
    linesBefore: Int,
    linesAfter: Int
)

case class LanguagesConfig(
    haskell: LanguageConfig,
    ruby: LanguageConfig,
    rust: LanguageConfig,
    javascript: LanguageConfig
)

case class LanguageConfig(
    repository: String,
    repoIndexUrl: URI,
    concurrentTasksCount: Int
)

case class MetricsConfig(
    enableMatomoMetrics: Boolean
)

object Config extends ConfigReaders {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[Config] = loadConfigF[F, Config]

}
