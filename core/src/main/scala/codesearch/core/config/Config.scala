package codesearch.core.config

import java.net.URI
import java.nio.file.Path

import cats.effect.Sync
import pureconfig.module.catseffect._
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait RepositoryConfig {
  def repository: String
  def repoIndexUrl: URI
}

trait ArchivedIndexConfig extends RepositoryConfig {
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
    downloaderConfig: PackageDownloaderConfig
) extends ArchivedIndexConfig

case class RubyConfig(
    repository: String,
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoJsonPath: Path,
    scriptPath: Path,
    downloaderConfig: PackageDownloaderConfig
) extends ArchivedIndexConfig

case class RustConfig(
    repository: String,
    repoIndexUrl: URI,
    repoArchivePath: Path,
    repoPath: Path,
    ignoreFiles: Set[String],
    downloaderConfig: PackageDownloaderConfig
) extends ArchivedIndexConfig

case class JavaScriptConfig(
    repository: String,
    repoIndexUrl: URI,
    downloaderConfig: PackageDownloaderConfig
) extends RepositoryConfig

case class SourcesFilterConfig(
    allowedFileNames: Set[String]
)

case class IndexDownloaderConfig(

                                )

case class PackageDownloaderConfig(
    packageUrl: String,
    packageArchivePath: String,
    packageSourcesPath: String,
    filterConfig: SourcesFilterConfig
)

case class RateLimitConfig(
                              numberTasks: Int,
                              seconds
                          )

case class MetricsConfig(
    enableMatomoMetrics: Boolean
)

object Config extends ConfigReaders {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def load[F[_]: Sync]: F[Config] = loadConfigF[F, Config]

}
