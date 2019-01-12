package codesearch.core.config

import cats.effect.Sync
import pureconfig.module.catseffect._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

case class Config(db: DatabaseConfig, snippetConfig: SnippetConfig, languagesConfig: LanguagesConfig)
case class DatabaseConfig(
    dataSourceClass: String,
    port: Int,
    name: String,
    user: String,
    password: String
)
case class SnippetConfig(pageSize: Int, linesBefore: Int, linesAfter: Int)

case class LanguagesConfig(
    haskellConfig: HaskellConfig,
    rubyConfig: RubyConfig,
    rustConfig: RustConfig,
    javaScriptConfig: JavaScriptConfig
)

case class HaskellConfig(concurrentTasksCount: Int)
case class RubyConfig(concurrentTasksCount: Int)
case class RustConfig(concurrentTasksCount: Int)
case class JavaScriptConfig(concurrentTasksCount: Int)

object Config {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[Config] = loadConfigF[F, Config]

}
