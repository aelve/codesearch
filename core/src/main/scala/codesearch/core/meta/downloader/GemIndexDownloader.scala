package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.config.RubyConfig
import codesearch.core.db.repository.PackageIndexDbRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.RubyIndexUnarchiver
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object GemIndexDownloader {
  def apply[F[_]: Sync: ContextShift](
      config: RubyConfig,
      downloader: Downloader[F],
      xa: Transactor[F]
  ): F[RepositoryIndexDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield
      new ArchivedIndexDownloader(
        config,
        downloader,
        RubyIndexUnarchiver(config),
        PackageIndexDbRepository(xa),
        logger
      )
}
