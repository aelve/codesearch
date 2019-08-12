package integration.fakes

import java.nio.file.Path
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.index.repository.Downloader
import com.softwaremill.sttp.Uri
import fs2.Stream
import fs2.io.file

case class FakeDownloader[F[_]: Sync: ContextShift](data: Stream[F, Byte]) extends Downloader[F] {

  /**
    * Function download sources from remote resource and save in file.
    *
    * @param from is uri of remote resource.
    * @param to is path for downloaded file. If the file does not exist for this path will be create.
    * @return downloaded archive file with sources.
    */
  def download(from: Uri, to: Path): F[Path] =
    Sync[F].delay(to.getParent.toFile.mkdirs) >> data
      .through(file.writeAll(to, BlockingEC, List(CREATE, TRUNCATE_EXISTING)))
      .compile
      .drain
      .as(to)

  def download(from: Uri): Stream[F, Byte] = data
}
