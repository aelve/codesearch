package codesearch.core.util

import java.nio.file.Path

import cats.effect.Sync
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

trait Unarchiver[F[_]] {
  def extract(
      archive: Path,
      to: Path,
      format: ArchiveFormat,
      compressionType: CompressionType
  ): F[Unit]

  def extract(
      archive: Path,
      to: Path,
      format: ArchiveFormat
  ): F[Unit]
}

object Unarchiver {
  def apply[F[_]](implicit F: Sync[F]): Unarchiver[F] = new Unarchiver[F] {
    def extract(
        archive: Path,
        to: Path,
        format: ArchiveFormat,
        compressionType: CompressionType
    ): F[Unit] = F.delay {
      ArchiverFactory
        .createArchiver(format, compressionType)
        .extract(archive.toFile, to.toFile)
    }

    def extract(archive: Path, to: Path, archiveFormat: ArchiveFormat): F[Unit] = F.delay {
      ArchiverFactory
        .createArchiver(archiveFormat)
        .extract(archive.toFile, to.toFile)
    }
  }
}
