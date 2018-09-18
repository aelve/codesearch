package codesearch.core.index.repository

import java.io.File
import java.nio.file.Path

import cats.Eval
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Directory.ops._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{Uri, _}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.applicative._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

object SourceRepository {

  private val logger: Logger = LoggerFactory.getLogger(SourceRepository.getClass)

  private implicit val asyncSttp: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  final case class DownloadException(message: String) extends Throwable(message)

  implicit def packageDownloader[A <: SourcePackage: Extensions: Directory]: Download[A] =
    (pack: A) => {
      for {
        archive   <- download(pack.url, pack.archive)
        directory <- pack.extract(archive, pack.unarchived)
        _         <- Future(deleteExcessFiles(directory, FileFilter.create[A]))
      } yield directory
    }.andThen { case Failure(ex) => logger.error(ex.getMessage) }

  /**
    * Function download sources from remote resource and save in file.
    *
    * @param from is uri of remote resource.
    * @param path is path for downloaded file. If the file does not exist for this path will be create.
    * @return downloaded archive file with sources.
    */
  private def download(from: Uri, path: Path): Future[File] = {
    sttp
      .get(from)
      .response(asByteArray)
      .send
      .flatMap(_.body.fold(
        error => Future.failed(DownloadException(error)),
        result => {
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
    * @param fileFilter is implementation of trait [[FileFilter]]
    * @return count removed files
    */
  private def deleteExcessFiles[A](directory: Path, fileFilter: FileFilter[A]): Int = {
    def deleteRecursively(dir: File, predicate: File => Boolean): Eval[Int] = {
      for {
        (dirs, files)      <- Eval.later(dir.listFiles.toList.partition(_.isDirectory))
        filesDeleted       <- files.filterNot(predicate).traverse(file => Eval.later(file.delete)).map(_.size)
        nestedFilesDeleted <- dirs.traverse(dir => deleteRecursively(dir, predicate)).map(_.size)
        _                  <- Eval.later(dir.delete).whenA(dir.listFiles.isEmpty)
      } yield filesDeleted + nestedFilesDeleted
    }
    deleteRecursively(directory.toFile, fileFilter.filter).value
  }
}
