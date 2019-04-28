package codesearch.core.meta.unarchiver

import java.nio.file.Path

import cats.Order
import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.functor._
import codesearch.core.config.HaskellConfig
import codesearch.core.db.repository.PackageIndexTableRow
import codesearch.core.model.Version
import codesearch.core.util.Unarchiver
import fs2.{Chunk, Stream}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.CompressionType.GZIP

private[meta] final class HaskellIndexUnarchiver[F[_]: Sync](
    unarchiver: Unarchiver[F],
    config: HaskellConfig
) extends StreamIndexUnarchiver[F] {

  def unarchive(path: Path): F[Stream[F, PackageIndexTableRow]] = {
    for {
      _ <- unarchiver.extract(path, config.repoPath, TAR, GZIP)
    } yield flatPackages
  }

  private def flatPackages: F[Stream[F, PackageIndexTableRow]] = {
    Sync[F].pure(
      Stream
        .evalUnChunk(Sync[F].delay(Chunk.array(config.repoPath.toFile.listFiles)))
        .filter(_.isDirectory)
        .evalMap { packageDir =>
          Sync[F].delay {
            packageDir.listFiles.toList
              .filter(_.isDirectory)
              .map(_.getName)
              .maximumOption(Order.fromLessThan(Version.less))
              .map(version => PackageIndexTableRow(packageDir.getName, version, config.repository))
          }
        }
        .unNone
    )
  }
}

object HaskellIndexUnarchiver {
  def apply[F[_]: ConcurrentEffect: ContextShift](
      unarchiver: Unarchiver[F],
      config: HaskellConfig
  ): HaskellIndexUnarchiver[F] = new HaskellIndexUnarchiver(unarchiver, config)
}
