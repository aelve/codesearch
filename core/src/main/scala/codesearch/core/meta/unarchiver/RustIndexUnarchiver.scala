package codesearch.core.meta.unarchiver

import java.nio.file.Path

import cats.effect.Sync
import cats.syntax.functor._
import codesearch.core.config.RustConfig
import codesearch.core.db.repository.PackageIndex
import codesearch.core.util.{FsUtils, Unarchiver}
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import org.rauschig.jarchivelib.ArchiveFormat.ZIP

final class RustIndexUnarchiver[F[_]: Sync](
    unarchiver: Unarchiver[F],
    config: RustConfig
) extends StreamIndexUnarchiver[F] {

  private implicit val packageDecoder: Decoder[PackageIndex] = { cursor =>
    for {
      name    <- cursor.get[String]("name")
      version <- cursor.get[String]("vers")
    } yield PackageIndex(name, version, config.repository)
  }

  def unarchive(path: Path): F[Stream[F, PackageIndex]] = {
    for {
      _ <- unarchiver.extract(path, config.repoPath, ZIP)
    } yield flatPackages
  }

  private def flatPackages: F[Stream[F, PackageIndex]] = {
    Sync[F].delay(
      FsUtils
        .recursiveListFiles(config.repoPath.toFile)
        .filter(file => !config.ignoreFiles.contains(file.getName))
        .evalMap(file => FsUtils.readFileAsync(file.getAbsolutePath).map(_.last))
        .through(stringStreamParser)
        .through(decoder[F, PackageIndex]))
  }
}

object RustIndexUnarchiver {
  def apply[F[_]: Sync](
      unarchiver: Unarchiver[F],
      config: RustConfig
  ): RustIndexUnarchiver[F] = new RustIndexUnarchiver(unarchiver, config)
}
