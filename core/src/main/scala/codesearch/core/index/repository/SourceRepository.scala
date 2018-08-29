package codesearch.core.index.repository
import java.io.File
import java.nio.file.Path

import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.PackageDirectory._
import codesearch.core.index.repository.Extensions.Extension
import com.softwaremill.sttp.{Uri, _}
import org.apache.commons.io.FilenameUtils.getExtension

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SourceRepository {

  final case class DownloadException(
      message: String
  ) extends Throwable(message)

  implicit def packageDownloader[A <: SourcePackage: Extension: Directory]: Download[A] =
    (pack: A) => {
      for {
        zipped    <- download(pack.url, pack.archive)
        directory <- pack.extract(zipped, pack.unarchived)
        _         <- deleteExcessFiles(directory, Extension[A].extensions)
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
        error => Future.failed(DownloadException(error)),
        result => Future.successful(result)
      ))
  }

  /**
    * Function removes all extension files that are not contained in the set of allowed extensions
    *
    * @param directory is root directory of specific package
    * @param allowedExtentions is extensions defined for each language
    * @return count removed files
    */
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
