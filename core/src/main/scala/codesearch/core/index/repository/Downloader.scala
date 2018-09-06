package codesearch.core.index.repository
import java.io.File
import java.nio.file.Path

import com.softwaremill.sttp.{SttpBackend, Uri, asByteArray, sttp}
import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

trait Downloader[F[_]] {
  def download(from: Uri, to: Path): F[File]
}

final case class DownloadException(message: String) extends Throwable(message)

final class FileDownloader(implicit ec: ExecutionContext, http: SttpBackend[Future, Nothing])
    extends Downloader[Future] {

  /**
    * Function download sources from remote resource and save in file.
    *
    * @param from is uri of remote resource.
    * @param to is path for downloaded file. If the file does not exist for this path will be create.
    * @return downloaded archive file with sources.
    */
  override def download(from: Uri, to: Path): Future[File] = {
    sttp
      .get(from)
      .response(asByteArray)
      .send
      .flatMap(
        _.body.fold(
          error => Future.failed(DownloadException(error)),
          result => {
            val archive = to.toFile
            FileUtils.writeByteArrayToFile(archive, result)
            Future.successful(archive)
          }
        ))
  }
}

object FileDownloader {
  def apply[F[_]: Downloader]: Downloader[F] = implicitly
}
