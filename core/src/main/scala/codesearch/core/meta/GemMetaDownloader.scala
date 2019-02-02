package codesearch.core.meta

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.RubyConfig
import codesearch.core.index.repository.Downloader
import com.softwaremill.sttp._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.sys.process._

class GemMetaDownloader[F[_]: Sync](config: RubyConfig, downloader: Downloader[F], logger: Logger[F])
    extends MetaDownloader[F] {

  def downloadMeta: F[Unit] =
    for {
      _ <- logger.info("Downloading ruby meta information")
      _ <- downloader.download(Uri(config.repoIndexUrl), config.repoArchivePath)
      _ <- Sync[F].delay {
        Seq("ruby", config.scriptPath.toString, config.repoArchivePath.toString, config.repoJsonPath.toString) !!
      }
      _ <- logger.info("Downloading finished")
    } yield ()
}

object GemMetaDownloader {
  def apply[F[_]: Sync](config: RubyConfig, downloader: Downloader[F]): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new GemMetaDownloader(config, downloader, logger)
}
