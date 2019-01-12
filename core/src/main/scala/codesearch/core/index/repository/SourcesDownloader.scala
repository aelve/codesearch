package codesearch.core.index.repository

import java.io.File
import java.nio.file.Path

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Directory.ops._

trait SourcesDownloader[F[_], A] {
  def downloadSources(pack: A): F[Path]
}

object SourcesDownloader {

  implicit def apply[F[_]: Downloader, A <: SourcePackage: FileFilter: Directory](
      implicit F: Sync[F]
  ): SourcesDownloader[F, A] =
    new SourcesDownloader[F, A] {

      def downloadSources(pack: A): F[Path] =
        for {
          archive   <- Downloader[F].download(pack.url, pack.archive)
          directory <- pack.extract(archive, pack.unarchived)
          _         <- deleteExcessFiles(directory)
        } yield directory

      /**
        * Function removes all extension files that are not contained in the set of allowed extensions
        *
        * @param directory is root directory of specific package
        * @return count removed files
        */
      def deleteExcessFiles(directory: Path): F[Int] = {
        def deleteRecursively(dir: File, predicate: File => Boolean): F[Int] = {
          for {
            (dirs, files)      <- F.delay(dir.listFiles.toList.partition(_.isDirectory))
            filesDeleted       <- files.filterNot(predicate).traverse(file => F.delay(file.delete)).map(_.size)
            nestedFilesDeleted <- dirs.traverse(dir => deleteRecursively(dir, predicate)).map(_.size)
            _                  <- F.delay(dir.delete).whenA(dir.listFiles.isEmpty)
          } yield filesDeleted + nestedFilesDeleted
        }
        deleteRecursively(directory.toFile, FileFilter[A].filter)
      }
    }
}
