package codesearch.core.meta

import cats.effect.{ConcurrentEffect, ContextShift}
import cats.syntax.functor._
import codesearch.core.config.LanguageConfig
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.HaskellIndexUnarchiver
import doobie.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

final class HackageMetaDownloader[F[_]: ConcurrentEffect: ContextShift](
    config: LanguageConfig,
    downloader: Downloader[F],
    xa: Transactor[F],
    logger: Logger[F]
) extends IndexDownloader[F](
      config,
      downloader,
      xa,
      logger,
      HaskellIndexUnarchiver(config)
    )

object HackageMetaDownloader {
  def apply[F[_]: ConcurrentEffect: ContextShift](
      config: LanguageConfig,
      downloader: Downloader[F],
      xa: Transactor[F]
  ): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new HackageMetaDownloader(config, downloader, xa, logger)
}
