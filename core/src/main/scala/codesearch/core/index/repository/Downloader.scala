package codesearch.core.index.repository

import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}

import cats.effect.ExitCase.Error
import cats.effect.syntax.bracket._
import cats.effect.{ContextShift, Sync}
import cats.syntax.either._
import cats.syntax.functor._
import codesearch.core._
import com.softwaremill.sttp.{SttpBackend, Uri, asStream, sttp}
import fs2.io.file
import fs2.{Chunk, Stream}
import org.apache.commons.io.FileUtils

import scala.concurrent.duration.Duration

trait Downloader[F[_]] {
  def download(from: Uri, to: Path): F[Path]
  def download(from: Uri): Stream[F, Byte]
}

object Downloader {

  final case class DownloadException(message: String) extends Throwable(message)

  def apply[F[_]: Downloader]: Downloader[F] = implicitly

  def create[F[_]: ContextShift](
      implicit http: SttpBackend[F, Stream[F, ByteBuffer]],
      F: Sync[F]
  ): Downloader[F] =
    new Downloader[F] {

      /**
        * Function download sources from remote resource and save in file.
        *
        * @param from is uri of remote resource.
        * @param to is path for downloaded file. If the file does not exist for this path will be create.
        * @return downloaded archive file with sources.
        */
      def download(from: Uri, to: Path): F[Path] = {
        val packageDir = to.toFile.getParentFile
        F.delay(packageDir.mkdirs).bracketCase(_ => save(from, to).as(to)) {
          case (_, Error(_)) => F.delay(FileUtils.deleteDirectory(packageDir.getParentFile))
          case _             => F.unit
        }
      }

      def download(from: Uri): Stream[F, Byte] =
        Stream.eval {
          sttp
            .get(from)
            .response(asStream[Stream[F, ByteBuffer]])
            .readTimeout(Duration.Inf)
            .send
        }.flatMap { response =>
          response.body
            .leftMap(DownloadException)
            .fold(Stream.raiseError(_), _.flatMap(buffer => Stream.chunk(Chunk.array(buffer.array))))
        }

      private def save(from: Uri, to: Path): F[Unit] =
        download(from).through(file.writeAll(to, BlockingEC, List(CREATE, TRUNCATE_EXISTING))).compile.drain
    }
}
