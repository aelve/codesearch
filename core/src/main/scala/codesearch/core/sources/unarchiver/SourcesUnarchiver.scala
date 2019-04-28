package codesearch.core.sources.unarchiver

import java.nio.file.Path

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.apache.commons.io.FileUtils.{moveDirectoryToDirectory, moveFileToDirectory}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType.GZIP

trait SourcesUnarchiver[F[_]] {

  /** Return directory containing all unarchived files and directories
    *
    * @param archive is file to unarchiving
    * @param directory is target directory
    * @return directory containing all unarchived files and directories
    */
  def unarchive(archive: Path, directory: Path): F[Path]
}

object SourcesUnarchiver {
  def apply[F[_]: Sync]: SourcesUnarchiver[F] = new SourcesUnarchiver[F]() {
    def unarchive(archive: Path, directory: Path): F[Path] = {
      for {
        _    <- Sync[F].delay(ArchiverFactory.createArchiver(TAR, GZIP).extract(archive.toFile, directory.toFile))
        path <- flatDir(directory)
      } yield path
    }

    private def flatDir(unarchived: Path): F[Path] = Sync[F].delay {
      val dir              = unarchived.toFile
      val notCreateDestDir = false
      dir.listFiles
        .filter(_.isDirectory)
        .foreach(_.listFiles.foreach(file =>
          if (file.isDirectory) moveDirectoryToDirectory(file, dir, notCreateDestDir)
          else moveFileToDirectory(file, dir, notCreateDestDir)))
      unarchived
    }
  }
}
