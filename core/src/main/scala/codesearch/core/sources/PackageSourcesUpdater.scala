package codesearch.core.sources

import cats.effect.Sync
import codesearch.core.config.RepositoryConfig
import cats.syntax.applicative._
import cats.syntax.functor._
import codesearch.core.db.repository.{Package, PackageIndexDbRepository}
import codesearch.core.sources.downloader.SourcesDownloader

trait SourcesUpdater[F[_]] {
  def update: F[Unit]
}

class PackageSourcesUpdater[F[_]: Sync](
    config: RepositoryConfig,
    indexRep: PackageIndexDbRepository[F],
    downloader: SourcesDownloader[F, A]
) extends SourcesUpdater[F] {

  def update: F[Unit] = {
    for {
      newIndexes <- indexRep.findNew(config.repository).pure[F]

    } yield ()
  }

  private def download(`package`: Package): F[Unit] = {}
}
