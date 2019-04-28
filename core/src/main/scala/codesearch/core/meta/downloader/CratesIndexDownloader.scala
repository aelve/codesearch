package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.config.RustConfig
import codesearch.core.db.repository.PackageIndexDbRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.RustIndexUnarchiver
import codesearch.core.util.Unarchiver
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object CratesIndexDownloader {
  def apply[F[_]: Sync: ContextShift](
      config: RustConfig,
      unarchiver: Unarchiver[F],
      downloader: Downloader[F],
      xa: Transactor[F]
  ): F[RepositoryIndexDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield
      new ArchivedIndexDownloader(
        config,
        downloader,
        RustIndexUnarchiver(unarchiver, config),
        PackageIndexDbRepository(xa),
        logger
      )
}
