package codesearch.core.sources.downloader

import java.nio.file.Paths

import cats.effect.{Concurrent, Sync, Timer}
import codesearch.core.config.PackageDownloaderConfig
import codesearch.core.db.repository.{PackageDbRepository, PackageIndexTableRow}
import codesearch.core.index.repository.Downloader
import codesearch.core.sources.filter.FileFilter
import codesearch.core.sources.unarchiver.SourcesUnarchiver
import com.softwaremill.sttp.Uri
import io.chrisdavenport.log4cats.Logger
import upperbound.{Limiter, Rate}
import cats.effect.concurrent.Deferred
import upperbound.syntax.rate._


import scala.concurrent.duration._

object RateLimitedSourcesDownloader {
  def apply[F[_]: Concurrent: Timer](
      downloader: Downloader[F],
      unarchiver: SourcesUnarchiver[F],
      fileFilter: FileFilter[F],
      packageDbRepository: PackageDbRepository[F],
      config: PackageDownloaderConfig,
      logger: Logger[F]
  ): SourcesDownloader[F] = new SourcesDownloader[F] {

    def download(index: PackageIndexTableRow): F[Unit] = {
      val packageUrl  = Uri(config.packageUrl.format(index.name, index.version))
      val archivePath = Paths.get(config.packageArchivePath.format(index.name, index.version))
      val sourcesPath = Paths.get(config.packageSourcesPath.format(index.name, index.version))
      for {
        _          <- logger.info(s"Downloading ${index.name}-${index.version} sources")
        archive    <- downloader.download(packageUrl, archivePath)
        sourcesDir <- unarchiver.unarchive(archive, sourcesPath)
        _          <- fileFilter.filter(sourcesDir)
        _          <- packageDbRepository.upsert(index.name, index.version, index.repository)
      } yield ()
    }

    private def rateLimitedDownload: F[Unit] = {
      Limiter.start[F](maxRate = 10 every 1.seconds).use { implicit limiter =>
      }
    }
  }
}
