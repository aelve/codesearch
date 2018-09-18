package codesearch.core.index.repository
import java.io.File

import org.apache.commons.io.FilenameUtils.getExtension

trait FileFilter[A] {

  /** Function tells whether the file matches the defines predicate
    *
    * @param file is checkable file
    * @return true if file matches the defines predicate
    */
  def filter(file: File): Boolean
}

object FileFilter {
  def create[A](implicit E: Extensions[A]): FileFilter[A] = new FileFilter[A] {
    val maxFileSize: Int = 1024 * 1024
    val allowedFileNames = Set("makefile", "dockerfile", "readme", "changelog", "changes")
    override def filter(file: File): Boolean = {
      val fileName = file.getName.toLowerCase
      val fileExt  = getExtension(fileName)
      (if (fileExt.isEmpty) allowedFileNames.contains(fileName)
       else E.extensions.contains(fileExt)) && file.length < maxFileSize
    }
  }
}
