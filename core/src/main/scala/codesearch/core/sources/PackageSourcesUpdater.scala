package codesearch.core.sources

import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Sync, Timer}
import codesearch.core.config.{RateLimiterConfig, RepositoryConfig}
import codesearch.core.db.repository.PackageIndexRepository
import codesearch.core.sources.downloader.SourcesDownloader
import upperbound.syntax.rate._
import fs2.Stream
import upperbound.{Limiter, Rate}
import cats.syntax.all._

import scala.concurrent.duration._

trait SourcesUpdater[F[_]] {
  def update: F[Unit]
}

class PackageSourcesUpdater[F[_]: Concurrent: Timer](
    indexDbRepository: PackageIndexRepository[F],
    downloader: SourcesDownloader[F],
    rateLimiterConfig: Option[RateLimiterConfig] = None
) extends SourcesUpdater[F] {

  def update: F[Unit] = {
    val latestPackages = indexDbRepository.findLatestByRepository(config.repository)
    rateLimiterConfig match {
      case Some(rate) =>
    }

    for {
      d <- Deferred[F, A]
    } Limiter.start[F](maxRate = 10 every 1.seconds).use { limiter =>
      limiter.submit()

    }

  }

  private def updateStream: Stream[F, F[Unit]] =
    indexDbRepository
      .findLatestByRepository(config.repository)
      .map(downloader.download)

}
