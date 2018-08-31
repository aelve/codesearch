package codesearch.core.index.directory
import java.io.File
import java.nio.file.Path

import org.apache.commons.io.FileUtils.{moveDirectoryToDirectory, moveFileToDirectory}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType.GZIP

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[index] trait Extractor {

  /**
    * @param from is file to unarchiving
    * @param to is target directory
    */
  def unzippingMethod(from: File, to: File): Unit =
    ArchiverFactory
      .createArchiver(TAR, GZIP)
      .extract(from, to)

  /**
    * @param archive is file to unarchiving
    * @param directory is target directory
    * @return directory containing all unarchived files and directories
    */
  def extract(archive: File, directory: Path): Future[File] = Future {
    val unarchived = directory.toFile
    unzippingMethod(archive, unarchived)
    flatDir(unarchived)
  }

  /**
    * @param unarchived is directory contains unarchived files
    * @return same directory containing all files and directories from unarchived files
    */
  def flatDir(unarchived: File): File = {
    unarchived
      .listFiles()
      .filter(_.isDirectory)
      .foreach(_.listFiles().foreach(file =>
        if (file.isDirectory) moveDirectoryToDirectory(file, unarchived, false)
        else moveFileToDirectory(file, unarchived, false)))
    unarchived
  }
}
