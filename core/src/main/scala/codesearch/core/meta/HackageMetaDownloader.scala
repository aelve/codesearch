package codesearch.core.meta

import java.io.InputStream

import cats.Monad
import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.config.HaskellConfig
import codesearch.core.index.repository.Downloader
import codesearch.core.util.Unarchiver
import com.softwaremill.sttp._
import fs2.io.file
import fs2.{Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.CompressionType.GZIP

class HackageMetaDownloader[F[_]: Monad: Sync: ContextShift](
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

  def storeMeta: F[Unit] = {
    file
      .readAll[F](config.repoArchivePath, BlockingEC, chunkSize = 4096)
      .through(tarEntries)
      .through(flatPackages)
  }

  private final case class Package(name: String, version: String)

  private def tarEntries: Pipe[F, InputStream, TarArchiveEntry] = { input =>
    input.flatMap { inputStream =>
      Stream
        .bracket(Sync[F].delay(new TarArchiveInputStream(inputStream)))(tarStream => Sync[F].delay(tarStream.close()))
        .flatMap { tarStream =>
          Stream.unfoldEval[F, TarArchiveInputStream, TarArchiveEntry](tarStream) { tarStream =>
            Sync[F].delay {
              tarStream.getNextTarEntry match {
                case entry: TarArchiveEntry => Some(entry, tarStream)
                case _                      => None
              }
            }
          }
        }
    }
  }

  private def flatPackages: Pipe[F, TarArchiveEntry, Package] = { input =>
    input.flatMap { entry =>
      val parentName    = entry.getName
      val nestedEntries = entry.getDirectoryEntries
      Stream.emits(nestedEntries.map(nested => Package(parentName, nested.getName)))
    }
  }

  private def store: Pipe[F, Package, Unit] = { input =>
    val batchSize = 10000
    input.chunkN(batchSize).map { packages =>
      val packagesBatch = packages.toList

    } //etc
  }

}

object HackageMetaDownloader {
  def apply[F[_]: Sync: ContextShift](
      config: HaskellConfig,
      unarchiver: Unarchiver[F],
      downloader: Downloader[F]
  ): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new HackageMetaDownloader(config, unarchiver, downloader, logger)
}
