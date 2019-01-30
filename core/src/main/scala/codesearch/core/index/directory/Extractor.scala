package codesearch.core.index.directory

import java.io.File
import java.nio.file.Path

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.apache.commons.io.FileUtils.{moveDirectoryToDirectory, moveFileToDirectory}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType.GZIP

private[index] trait Extractor {

  /** Defines extraction method
    *
    * @param from is file to unarchiving
    * @param to is target directory
    */
  def unzipUsingMethod[F[_]](from: Path, to: Path)(implicit F: Sync[F]): F[Unit] = F.delay(
    ArchiverFactory
      .createArchiver(TAR, GZIP)
      .extract(from.toFile, to.toFile)
  )

  /** Return directory containing all unarchived files and directories
    *
    * @param archive is file to unarchiving
    * @param directory is target directory
    * @return directory containing all unarchived files and directories
    */
  def extract[F[_]: Sync](archive: Path, directory: Path): F[Path] =
    for {
      _    <- unzipUsingMethod(archive, directory)
      path <- flatDir(directory)
    } yield path

  /** Return same directory containing all files and directories from unarchived files
    *
    * @param unarchived is directory contains unarchived files
    * @return same directory containing all files and directories from unarchived files
    */
  def flatDir[F[_]](unarchived: Path)(implicit F: Sync[F]): F[Path] = F.delay {
    val dir = unarchived.toFile
    dir.listFiles
      .filter(_.isDirectory)
      .foreach(_.listFiles.foreach(file =>
        if (file.isDirectory) moveDirectoryToDirectory(file, dir, false)
        else moveFileToDirectory(file, dir, false)))
    unarchived
  }
}
