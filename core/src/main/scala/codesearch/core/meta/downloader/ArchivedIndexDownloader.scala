package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.ArchivedIndexConfig
import codesearch.core.db.repository.PackageIndexRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.StreamIndexUnarchiver
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger
import org.apache.commons.io.FileUtils

private[meta] class ArchivedIndexDownloader[F[_]: Sync: ContextShift](
    config: ArchivedIndexConfig,
    downloader: Downloader[F],
    unarchiver: StreamIndexUnarchiver[F],
    indexRepository: PackageIndexRepository[F],
    logger: Logger[F]
) extends RepositoryIndexDownloader[F] {

  def download: F[Unit] =
    for {
      _       <- logger.info(s"Downloading ${config.repository} meta information")
      archive <- downloader.download(Uri(config.repoIndexUrl), config.repoArchivePath)
      stream  <- unarchiver.unarchiveToStream(archive)
      _       <- indexRepository.batchUpsert(stream)
      _       <- Sync[F].delay(FileUtils.cleanDirectory(config.repoArchivePath.getParent.toFile))
      _       <- logger.info("Downloading finished")
    } yield ()
}
