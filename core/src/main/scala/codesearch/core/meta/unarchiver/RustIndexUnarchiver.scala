package codesearch.core.meta.unarchiver

import java.nio.file.Path

import cats.effect.Sync
import cats.syntax.functor._
import codesearch.core.config.RustConfig
import codesearch.core.db.repository.PackageIndexTableRow
import codesearch.core.util.{FsUtils, Unarchiver}
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import org.rauschig.jarchivelib.ArchiveFormat.ZIP

private[meta] final class RustIndexUnarchiver[F[_]: Sync](
    unarchiver: Unarchiver[F],
    config: RustConfig
) extends StreamIndexUnarchiver[F] {

  private implicit val packageDecoder: Decoder[PackageIndexTableRow] = { cursor =>
    for {
      name    <- cursor.get[String]("name")
      version <- cursor.get[String]("vers")
    } yield PackageIndexTableRow(name, version, config.repository)
  }

  def unarchive(path: Path): F[Stream[F, PackageIndexTableRow]] = {
    for {
      _ <- unarchiver.extract(path, config.repoPath, ZIP)
    } yield flatPackages
  }

  private def flatPackages: F[Stream[F, PackageIndexTableRow]] = {
    Sync[F].delay(
      FsUtils
        .recursiveListFiles(config.repoPath.toFile)
        .filter(file => !config.ignoreFiles.contains(file.getName))
        .evalMap(file => FsUtils.readFileAsync(file.getAbsolutePath).map(_.last))
        .through(stringStreamParser)
        .through(decoder[F, PackageIndexTableRow]))
  }
}

private[meta] object RustIndexUnarchiver {
  def apply[F[_]: Sync](
      unarchiver: Unarchiver[F],
      config: RustConfig
  ): RustIndexUnarchiver[F] = new RustIndexUnarchiver(unarchiver, config)
}
