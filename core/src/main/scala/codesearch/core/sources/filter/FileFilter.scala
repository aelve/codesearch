package codesearch.core.sources.filter

import java.io.File
import java.nio.file.Path

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import codesearch.core.index.repository.Extensions
import org.apache.commons.io.FilenameUtils.getExtension

trait FileFilter[F[_]] {
  def filter(dir: Path): F[Int]
}

object FileFilter {
  def apply[F[_]: Sync](
      extensions: Extensions,
      allowedFileNames: Set[String]
  ): FileFilter[F] = new FileFilter[F] {

    private val maxFileSize: Int = 1024 * 1024

    def filter(dir: Path): F[Int] = Sync[F].delay(filterRecursively(dir.toFile, filter))

    private def filterRecursively(dir: File, predicate: File => Boolean): F[Int] = {
      for {
        (dirs, files)      <- Sync[F].delay(dir.listFiles.toList.partition(_.isDirectory))
        filesDeleted       <- files.filterNot(predicate).traverse(file => Sync[F].delay(file.delete)).map(_.size)
        nestedFilesDeleted <- dirs.traverse(dir => filterRecursively(dir, predicate)).map(_.size)
        _                  <- Sync[F].delay(dir.delete).whenA(dir.listFiles.isEmpty)
      } yield filesDeleted + nestedFilesDeleted
    }

    private def filter(file: File): Boolean = {
      val fileName = file.getName.toLowerCase
      val fileExt  = getExtension(fileName)
      (if (fileExt.isEmpty) allowedFileNames.contains(fileName)
       else extensions.extensions.contains(fileExt)) && file.length < maxFileSize
    }
  }
}
