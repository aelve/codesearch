package codesearch.core.sources

import cats.effect.Sync
import codesearch.core.config.RepositoryConfig
import codesearch.core.db.repository.PackageIndexDbRepository
import codesearch.core.sources.downloader.SourcesDownloader

trait SourcesUpdater[F[_]] {
  def update: F[Unit]
}

class PackageSourcesUpdater[F[_]: Sync](
    config: RepositoryConfig,
    indexDbRepository: PackageIndexDbRepository[F],
    downloader: SourcesDownloader[F]
) extends SourcesUpdater[F] {

  def update: F[Unit] = {
    indexDbRepository
      .findNewByRepository(config.repository)
      .map(downloader.download)
      .compile
      .drain
  }
}
