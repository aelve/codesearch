package codesearch.core.meta.unarchiver

import java.nio.file.Path

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.config.RubyConfig
import codesearch.core.db.repository.PackageIndexTableRow
import fs2.Stream
import fs2.io.file
import io.circe.fs2.{byteArrayParser, decoder}

import scala.sys.process._

private[meta] final class RubyIndexUnarchiver[F[_]: Sync: ContextShift](
    config: RubyConfig
) extends StreamIndexUnarchiver[F] {

  def unarchiveToStream(path: Path): F[Stream[F, PackageIndexTableRow]] = {
    for {
      _ <- Sync[F].delay {
        Seq(
          "ruby",
          config.scriptPath.toString,
          path.toString,
          config.repoJsonPath.toString
        ) !!
      }
    } yield flatPackages
  }

  private def flatPackages: F[Stream[F, PackageIndexTableRow]] = {
    Sync[F].delay(
      file
        .readAll[F](config.repoJsonPath, BlockingEC, 4096)
        .through(byteArrayParser)
        .through(decoder[F, Seq[String]])
        .collect { case Seq(name, version, _) => PackageIndexTableRow(name, version, config.repository) })
  }
}

private[meta] object RubyIndexUnarchiver {
  def apply[F[_]: Sync: ContextShift](
      config: RubyConfig
  ): RubyIndexUnarchiver[F] = new RubyIndexUnarchiver(config)
}
