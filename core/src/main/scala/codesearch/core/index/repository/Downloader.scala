package codesearch.core.index.repository

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}

import cats.effect.ExitCase.Error
import cats.effect.{ContextShift, IO}
import cats.syntax.either.catsSyntaxEither
import codesearch.core._
import com.softwaremill.sttp.{SttpBackend, Uri, asStream, sttp}
import fs2.io.file
import fs2.{Chunk, Pipe, Sink, Stream}
import org.apache.commons.io.FileUtils

import scala.concurrent.duration.Duration

private[index] trait Downloader[F[_], O] {
  def download(from: Uri, to: Path): F[O]
}

private[index] trait StreamingDownloader[F[_], O] {
  def download(from: Uri): F[Stream[F, O]]
}

final case class DownloadException(message: String) extends Throwable(message)

private[index] final class ByteStreamDownloader(
    implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]]
) extends StreamingDownloader[IO, Byte] {

  override def download(from: Uri): IO[Stream[IO, Byte]] = stream(from).map(_.through(toBytes))

  private def stream(url: Uri): IO[Stream[IO, ByteBuffer]] = {
    sttp
      .get(url)
      .response(asStream[Stream[IO, ByteBuffer]])
      .readTimeout(Duration.Inf)
      .send
      .flatMap(response => IO.fromEither(response.body.leftMap(DownloadException)))
  }

  private def toBytes[F[_]]: Pipe[F, ByteBuffer, Byte] = { input =>
    input.flatMap(buffer => Stream.chunk(Chunk.array(buffer.array)))
  }
}

private[index] final class FileDownloader(
    implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    shift: ContextShift[IO]
) extends Downloader[IO, File] {

  /**
    * Function download sources from remote resource and save in file.
    *
    * @param from is uri of remote resource.
    * @param to is path for downloaded file. If the file does not exist for this path will be create.
    * @return downloaded archive file with sources.
    */
  override def download(from: Uri, to: Path): IO[File] = {
    val packageDir = to.toFile.getParentFile
    IO(packageDir.mkdirs).bracketCase(_ => save(from, to)) {
      case (_, Error(_)) => IO(FileUtils.deleteDirectory(packageDir.getParentFile))
      case _             => IO.unit
    }
  }

  private def save(from: Uri, to: Path): IO[File] = {
    new ByteStreamDownloader()
      .download(from)
      .flatMap(_.through(toFile(to)).compile.drain)
      .map(_ => to.toFile)
  }

  private def toFile(path: Path): Sink[IO, Byte] =
    file.writeAll(path, BlockingEC, List(CREATE, TRUNCATE_EXISTING))
}

object FileDownloader {
  def apply[F[_], O](implicit d: Downloader[F, O]): Downloader[F, O] = d
}
