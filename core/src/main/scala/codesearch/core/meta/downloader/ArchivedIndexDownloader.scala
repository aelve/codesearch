package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.IndexArchiveConfig
import codesearch.core.db.repository.PackageIndexRep
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.StreamIndexUnarchiver
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger
import org.apache.commons.io.FileUtils

private[meta] class ArchivedIndexDownloader[F[_]: Sync: ContextShift](
    config: IndexArchiveConfig,
    downloader: Downloader[F],
    unarchiver: StreamIndexUnarchiver[F],
    indexRep: PackageIndexRep[F],
    logger: Logger[F]
) extends RepositoryIndexDownloader[F] {

  def download: F[Unit] =
    for {
      _      <- logger.info(s"Downloading ${config.repository} meta information")
      path   <- downloader.download(Uri(config.repoIndexUrl), config.repoArchivePath)
      stream <- unarchiver.unarchive(path)
      _      <- indexRep.insertIndexes(stream)
      _      <- Sync[F].delay(FileUtils.cleanDirectory(config.repoArchivePath.getParent.toFile))
      _      <- logger.info("Downloading finished")
    } yield ()
}
