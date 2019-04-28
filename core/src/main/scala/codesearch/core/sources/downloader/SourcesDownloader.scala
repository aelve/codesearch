package codesearch.core.sources.downloader

import java.nio.file.Paths

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.RepositoryConfig
import codesearch.core.db.repository.{PackageDbRepository, PackageIndexTableRow, PackageTableRow}
import codesearch.core.index.repository.Downloader
import codesearch.core.sources.filter.FileFilter
import codesearch.core.sources.unarchiver.SourcesUnarchiver
import com.softwaremill.sttp._
import io.chrisdavenport.log4cats.Logger

trait SourcesDownloader[F[_]] {
  def downloadSources(packageIndex: PackageIndexTableRow): F[Unit]
}

object SourcesDownloader {
  def apply[F[_]: Sync](
      downloader: Downloader[F],
      unarchiver: SourcesUnarchiver[F],
      fileFilter: FileFilter[F],
      packageDbRepository: PackageDbRepository[F],
      config: RepositoryConfig,
      logger: Logger[F]
  ): SourcesDownloader[F] = (index: PackageIndexTableRow) => {
    val packageUrl = uri"${config.packageUrl.format(index.name, index.version)}"
    for {
      _          <- logger.info(s"Downloading ${index.name}-${index.version} sources")
      archive    <- downloader.download(packageUrl, Paths.get(""))
      sourcesDir <- unarchiver.unarchive(archive)
      _          <- fileFilter.filter(sourcesDir)
      _          <- packageDbRepository.upsert(PackageTableRow(index.name, index.version, index.repository))
    } yield ()
  }
}
