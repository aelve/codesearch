package codesearch.core.meta.downloader

import cats.effect.{ConcurrentEffect, ContextShift}
import codesearch.core.config.HaskellConfig
import cats.syntax.functor._
import codesearch.core.db.repository.PackageIndexDbRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.HaskellIndexUnarchiver
import codesearch.core.util.Unarchiver
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object HackageIndexDownloader {
  def apply[F[_]: ConcurrentEffect: ContextShift](
      config: HaskellConfig,
      downloader: Downloader[F],
      unarchiver: Unarchiver[F],
      xa: Transactor[F]
  ): F[RepositoryIndexDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield
      new ArchivedIndexDownloader(
        config,
        downloader,
        HaskellIndexUnarchiver(unarchiver, config),
        PackageIndexDbRepository(xa),
        logger
      )
}
