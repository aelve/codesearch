package codesearch.core.config

import java.net.URI
import java.nio.file.Path

import cats.effect.Sync
import pureconfig.module.catseffect._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait RemoteIndexConfig {
  def repository: String
  def repoIndexUrl: URI
}

trait IndexArchiveConfig extends RemoteIndexConfig {
  def repoArchivePath: Path
}

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

case class SnippetConfig(
    pageSize: Int,
    linesBefore: Int,
    linesAfter: Int
)

case class LanguagesConfig(
    haskell: HaskellConfig,
    ruby: RubyConfig,
    rust: RustConfig,
    javascript: JavaScriptConfig
)

case class HaskellConfig(
    repository: String,
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoPath: Path,
    concurrentTasksCount: Int
) extends IndexArchiveConfig

case class RubyConfig(
    repository: String,
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoJsonPath: Path,
    scriptPath: Path,
    concurrentTasksCount: Int
) extends IndexArchiveConfig

case class RustConfig(
    repository: String,
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoPath: Path,
    concurrentTasksCount: Int,
    ignoreFiles: Set[String]
) extends IndexArchiveConfig

case class JavaScriptConfig(
    repository: String,
    repoIndexUrl: URI,
    concurrentTasksCount: Int
) extends RemoteIndexConfig

case class MetricsConfig(
    enableMatomoMetrics: Boolean
)

object Config extends ConfigReaders {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[Config] = loadConfigF[F, Config]

}
