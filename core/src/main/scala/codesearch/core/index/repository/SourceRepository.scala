package codesearch.core.index.repository

import java.io.File
import java.nio.file.Path

import cats.Eval
import cats.effect.IO
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Directory.ops._
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.applicative._

final class SourceRepository[A <: SourcePackage: Extensions: Directory](downloader: Downloader[IO, File])
    extends Download[A] {

  override def downloadSources(pack: A): IO[Path] = {
    for {
      archive   <- downloader.download(pack.url, pack.archive)
      directory <- pack.extract(archive, pack.unarchived)
      _         <- deleteExcessFiles(directory, FileFilter.create[A])
    } yield directory
  }

  /**
    * Function removes all extension files that are not contained in the set of allowed extensions
    *
    * @param directory is root directory of specific package
    * @param fileFilter is implementation of trait [[FileFilter]]
    * @return count removed files
    */
  private def deleteExcessFiles(directory: Path, fileFilter: FileFilter): IO[Int] = {
    def deleteRecursively(dir: File, predicate: File => Boolean): Eval[Int] = {
      for {
        (dirs, files)      <- Eval.later(dir.listFiles.toList.partition(_.isDirectory))
        filesDeleted       <- files.filterNot(predicate).traverse(file => Eval.later(file.delete)).map(_.size)
        nestedFilesDeleted <- dirs.traverse(dir => deleteRecursively(dir, predicate)).map(_.size)
        _                  <- Eval.later(dir.delete).whenA(dir.listFiles.isEmpty)
      } yield filesDeleted + nestedFilesDeleted
    }
    IO.eval(deleteRecursively(directory.toFile, fileFilter.filter))
  }
}

object SourceRepository {
  def apply[A <: SourcePackage: Extensions: Directory](
      downloader: Downloader[IO, File]
  ): SourceRepository[A] = new SourceRepository[A](downloader)
}
