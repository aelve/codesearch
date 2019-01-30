package codesearch.core.meta

import cats.Monad
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.RustConfig
import codesearch.core.index.repository.Downloader
import codesearch.core.util.Unarchiver
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.rauschig.jarchivelib.ArchiveFormat.ZIP

class CratesMetaDownloader[F[_]: Monad](
    config: RustConfig,
    unarchiver: Unarchiver[F],
    downloader: Downloader[F],
    logger: Logger[F]
) extends MetaDownloader[F] {

  def downloadMeta: F[Unit] = {
    for {
      _       <- logger.info("Downloading rust meta information")
      archive <- downloader.download(Uri(config.repoIndexUrl), config.repoArchivePath)
      _       <- unarchiver.extract(archive, config.repoPath, ZIP)
      _       <- logger.info("Downloading finished")
    } yield ()
  }
}

object CratesMetaDownloader {
  def apply[F[_]: Sync](
      config: RustConfig,
      unarchiver: Unarchiver[F],
      downloader: Downloader[F]
  ): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new CratesMetaDownloader(config, unarchiver, downloader, logger)
}
