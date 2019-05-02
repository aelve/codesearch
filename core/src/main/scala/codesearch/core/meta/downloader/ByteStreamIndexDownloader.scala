package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.RepositoryConfig
import codesearch.core.db.repository.PackageIndexRepository
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.parser.IndexByteStreamParser
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger

private[meta] class ByteStreamIndexDownloader[F[_]: Sync: ContextShift](
    config: RepositoryConfig,
    downloader: Downloader[F],
    indexDbRepository: PackageIndexRepository[F],
    indexParser: IndexByteStreamParser[F],
    logger: Logger[F]
) extends RepositoryIndexDownloader[F] {

  def download: F[Unit] =
    for {
      _      <- logger.info(s"Downloading ${config.repository} meta information")
      stream <- downloader.download(Uri(config.repoIndexUrl)).pure[F].widen
      index  <- indexParser.parse(stream)
      _      <- indexDbRepository.batchUpsert(index)
      _      <- logger.info("Downloading finished")
    } yield ()
}
