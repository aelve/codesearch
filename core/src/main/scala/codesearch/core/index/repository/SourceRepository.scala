package codesearch.core.index.repository
import java.io.File
import java.nio.file.Path

import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.PackageDirectory._
import codesearch.core.index.repository.Extensions.Extension
import com.softwaremill.sttp.{Uri, _}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils.getExtension
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

object SourceRepository {

  private val logger: Logger = LoggerFactory.getLogger(SourceRepository.getClass)

  final case class DownloadException(
      message: String
  ) extends Throwable(message)

  implicit def packageDownloader[A <: SourcePackage: Extension: Directory]: Download[A] =
    (pack: A) => {
      for {
        archive   <- download(pack.url, pack.archive)
        directory <- pack.extract(archive, pack.unarchived)
        _         <- deleteExcessFiles(directory, Extension[A].extensions)
      } yield directory
    }.andThen { case Failure(ex) => logger.error(ex.getMessage) }

  private def download(from: Uri, path: Path): Future[File] = {
    implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
    Future {
      sttp.get(from).response(asByteArray).send().body
    }.flatMap(_.fold(
      error   => Future.failed(DownloadException(error)),
      result  => {
        val archive = path.toFile
        FileUtils.writeByteArrayToFile(archive, result)
        Future.successful(archive)
      }
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
      case Nil => excess
      case file :: others if file.isDirectory =>
        if (file.listFiles().length > 0)
          filterFiles(others ++ file.listFiles(), excess)
        else filterFiles(others, file :: excess)
      case file :: others =>
        if (allowedExtentions.contains(getExtension(file.getName)))
          filterFiles(others, excess)
        else filterFiles(others, file :: excess)
    }
    filterFiles(List(directory)).map(_.delete()).count(identity)
  }
}
