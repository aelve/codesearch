package codesearch.core.sources.downloader

import codesearch.core.index.repository.Downloader
import cats.syntax.functor._
import codesearch.core.config.HaskellConfig
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object HaskellSourceDownloader {
  def apply[F[_]](
      downloader: Downloader[F],
      config: HaskellConfig
  ): SourcesDownloader[F] =
    for {
      logger <- Slf4jLogger.create
    } yield SourcesDownloader(
      downloader,

    )
}
