package codesearch.core.sources.downloader

import java.nio.file.Paths

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.config.PackageDownloaderConfig
import codesearch.core.db.repository.{PackageDbRepository, PackageIndexTableRow}
import codesearch.core.index.repository.Downloader
import codesearch.core.sources.filter.FileFilter
import codesearch.core.sources.unarchiver.SourcesUnarchiver
import com.softwaremill.sttp._
import io.chrisdavenport.log4cats.Logger

trait SourcesDownloader[F[_]] {
  def download(index: PackageIndexTableRow): F[Unit]
}

object SourcesDownloader {
  def apply[F[_]: Sync](
      downloader: Downloader[F],
      unarchiver: SourcesUnarchiver[F],
      fileFilter: FileFilter[F],
      packageDbRepository: PackageDbRepository[F],
      config: PackageDownloaderConfig,
      logger: Logger[F]
  ): SourcesDownloader[F] = (index: PackageIndexTableRow) => {
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
}
