package codesearch.core.sources.downloader

import cats.effect.Sync
import cats.syntax.functor._
import codesearch.core.config.PackageDownloaderConfig
import codesearch.core.db.repository.PackageDbRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.index.repository.Extensions.JavaScriptExtensions
import codesearch.core.sources.filter.FileFilter
import codesearch.core.sources.unarchiver.SourcesUnarchiver
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object JavaScriptSourcesDownloader {
  def apply[F[_]: Sync](
      downloader: Downloader[F],
      packageDbRepository: PackageDbRepository[F],
      downloaderConfig: PackageDownloaderConfig
  ): F[SourcesDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield
      SourcesDownloader(
        downloader,
        SourcesUnarchiver[F],
        FileFilter[F](JavaScriptExtensions, downloaderConfig.filterConfig.allowedFileNames),
        packageDbRepository,
        downloaderConfig,
        logger
      )
}
