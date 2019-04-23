package codesearch.core.meta.downloader

import cats.effect.{ContextShift, Sync}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.RemoteIndexConfig
import codesearch.core.db.repository.PackageIndexRep
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.parser.IndexByteStreamParser
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger

class ByteStreamIndexDownloader[F[_]: Sync: ContextShift](
    config: RemoteIndexConfig,
    downloader: Downloader[F],
    indexRep: PackageIndexRep[F],
    indexParser: IndexByteStreamParser[F],
    logger: Logger[F]
) extends RepositoryIndexDownloader[F] {

  def download: F[Unit] =
    for {
      _      <- logger.info(s"Downloading ${config.repository} meta information")
      stream <- downloader.download(Uri(config.repoIndexUrl)).pure[F]
      index  <- indexParser.parse(stream)
      _      <- indexRep.insertIndexes(index)
      _      <- logger.info("Downloading finished")
    } yield ()
}
