package codesearch.core.meta.unarchiver

import java.nio.file.Path

import cats.effect.{ContextShift, Sync}
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.config.RubyConfig
import codesearch.core.db.repository.PackageIndex
import fs2.Stream
import fs2.io.file
import io.circe.fs2.{byteArrayParser, decoder}

import scala.sys.process._

final class RubyIndexUnarchiver[F[_]: Sync: ContextShift](config: RubyConfig) extends StreamIndexUnarchiver[F] {

  def unarchive(path: Path): F[Stream[F, PackageIndex]] = {
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

  private def flatPackages: F[Stream[F, PackageIndex]] = {
    Sync[F].delay(
      file
        .readAll[F](config.repoJsonPath, BlockingEC, 4096)
        .through(byteArrayParser)
        .through(decoder[F, Seq[String]])
        .collect { case Seq(name, version, _) => PackageIndex(name, version, config.repository) })
  }
}

object RubyIndexUnarchiver {
  def apply[F[_]: Sync: ContextShift](
      config: RubyConfig
  ): RubyIndexUnarchiver[F] = new RubyIndexUnarchiver(config)
}
