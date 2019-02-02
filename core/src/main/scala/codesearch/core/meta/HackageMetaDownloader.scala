package codesearch.core.meta

import cats.Monad
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.HaskellConfig
import codesearch.core.index.repository.Downloader
import codesearch.core.util.Unarchiver
import com.softwaremill.sttp._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.CompressionType.GZIP

class HackageMetaDownloader[F[_]: Monad](
    config: HaskellConfig,
    unarchiver: Unarchiver[F],
    downloader: Downloader[F],
    logger: Logger[F]
) extends MetaDownloader[F] {

  def downloadMeta: F[Unit] =
    for {
      _ <- logger.info("Downloading haskell meta information")
      _ <- downloader.download(Uri(config.repoIndexUrl), config.repoArchivePath)
      _ <- unarchiver.extract(config.repoArchivePath, config.repoPath, TAR, GZIP)
      _ <- logger.info("Downloading finished")
    } yield ()
}

object HackageMetaDownloader {
  def apply[F[_]: Sync](
      config: HaskellConfig,
      unarchiver: Unarchiver[F],
      downloader: Downloader[F]
  ): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new HackageMetaDownloader(config, unarchiver, downloader, logger)
}
