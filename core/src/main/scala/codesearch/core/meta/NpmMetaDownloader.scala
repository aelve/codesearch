package codesearch.core.meta

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.config.LanguageConfig
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.JavaScriptUnarchiver
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

final class NpmMetaDownloader[F[_]: Sync: ContextShift](
    config: LanguageConfig,
    downloader: Downloader[F],
    xa: Transactor[F],
    logger: Logger[F]
) extends IndexDownloader[F](
      config,
      downloader,
      xa,
      logger,
      JavaScriptUnarchiver(config)
    )

object NpmMetaDownloader {
  def apply[F[_]: Sync: ContextShift](
      config: LanguageConfig,
      downloader: Downloader[F],
      xa: Transactor[F]
  ): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new NpmMetaDownloader(config, downloader, xa, logger)
}
