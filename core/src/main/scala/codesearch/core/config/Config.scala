package codesearch.core.config

import java.net.URI
import java.nio.file.Path

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
    password: String
)

case class SnippetConfig(pageSize: Int, linesBefore: Int, linesAfter: Int)

case class LanguagesConfig(
    haskell: HaskellConfig,
    ruby: RubyConfig,
    rust: RustConfig,
    javascript: JavaScriptConfig
)

case class HaskellConfig(
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoPath: Path,
    concurrentTasksCount: Int
)

case class RubyConfig(
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoJsonPath: Path,
    scriptPath: Path,
    concurrentTasksCount: Int
)

case class RustConfig(
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoPath: Path,
    concurrentTasksCount: Int
)

case class JavaScriptConfig(
    repoIndexUrl: URI,
    repoJsonPath: Path,
    concurrentTasksCount: Int
)

case class MetricsConfig(
    enableMamotoMetrics: Boolean
)

object Config extends ConfigReaders {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[Config] = loadConfigF[F, Config]

}
