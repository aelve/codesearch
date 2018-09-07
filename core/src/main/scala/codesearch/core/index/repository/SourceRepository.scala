package codesearch.core.index.repository

import java.io.File
import java.nio.file.Path

import cats.Eval
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Directory.ops._
import org.slf4j.{Logger, LoggerFactory}
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.applicative._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

final class SourceRepository[A <: SourcePackage: Extensions: Directory](downloader: Downloader[Future])
    extends Download[A] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def downloadSources(pack: A): Future[Path] = {
    for {
      archive   <- downloader.download(pack.url, pack.archive)
      directory <- pack.extract(archive, pack.unarchived)
      _         <- Future(deleteExcessFiles(directory, FileFilter.create[A]))
    } yield directory
  }.andThen { case Failure(ex) => logger.error(ex.getMessage) }

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

object SourceRepository {
  def apply[A <: SourcePackage: Extensions: Directory](downloader: Downloader[Future]): SourceRepository[A] =
    new SourceRepository[A](downloader)
}
