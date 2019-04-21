package codesearch.core.meta

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.LanguageConfig
import codesearch.core.db.repository.{PackageIndex, PackageIndexRep}
import codesearch.core.index.repository.Downloader
import codesearch.core.meta.unarchiver.StreamIndexUnarchiver
import com.softwaremill.sttp.Uri
import doobie.util.transactor.Transactor
import fs2.Pipe
import io.chrisdavenport.log4cats.Logger

private[meta] trait MetaDownloader[F[_]] {

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def download: F[Unit]
}

private[meta] class IndexDownloader[F[_]: Sync: ContextShift](
    config: LanguageConfig,
    downloader: Downloader[F],
    xa: Transactor[F],
    logger: Logger[F],
    unarchiver: StreamIndexUnarchiver[F]
) extends MetaDownloader[F] {

  def download: F[Unit] =
    for {
      _ <- logger.info(s"Downloading ${config.repository} meta information")
      _ <- downloader
        .download(Uri(config.repoIndexUrl))
        .through(unarchiver.packages)
        .through(store)
        .compile
        .drain
      _ <- logger.info("Downloading finished")
    } yield ()

  private def store: Pipe[F, PackageIndex, Unit] = { input =>
    val batchSize       = 10000
    val packageIndexRep = PackageIndexRep[F](xa)
    input.chunkN(batchSize).map { packages =>
      packageIndexRep.insertRepIndexes(packages.toList)
    }
  }
}
