package codesearch.core.index.repository
import java.io.File
import java.nio.file.Path

import com.softwaremill.sttp.Uri
import org.apache.commons.io.FilenameUtils.getExtension

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.softwaremill.sttp._

object SourceRepository {

  final case class DownloadException(
      private val message: String
  ) extends Throwable(message)

  implicit def packageDownloader[A <: SourcePackage]: Download[A] =
    (pack: A) => {
      for {
        zipped    <- download(pack.url, pack.fsArchivePath)
        directory <- pack.extract(zipped, pack.fsUnzippedPath)
//        _         <- deleteExcessFiles(directory, pack.extensions)
      } yield directory
    }

  private def download(from: Uri, toPath: Path): Future[File] = {
    implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
    Future {
      sttp
        .get(from)
        .response(asFile(toPath.toFile))
        .send()
        .body
    }.flatMap(
      _.fold(
        error   => Future.failed(DownloadException(error)),
        result  => Future.successful(result)
      ))
  }

  private def deleteExcessFiles(directory: File, allowedExtentions: Set[String]): Future[Int] = Future {
    def filterFiles(all: List[File], excess: List[File] = Nil): List[File] = all match {
      case Nil                                => excess
      case file :: others if file.isDirectory => filterFiles(others ++ file.listFiles(), excess)
      case file :: others =>
        if (allowedExtentions.contains(getExtension(file.getName)))
          filterFiles(others, excess)
        else filterFiles(others, file :: excess)
    }
    filterFiles(List(directory)).map(_.delete()).count(identity)
  }
}
