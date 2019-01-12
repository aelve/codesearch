package codesearch.core.index.repository

import java.io.File

import codesearch.core.index.repository.Extensions._
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

  implicit def hackagePackageFileFilter: FileFilter[HackagePackage] = create(HaskellExtensions)
  implicit def cratesPackageFileFilter: FileFilter[CratesPackage]   = create(RustExtensions)
  implicit def npmPackageFileFilter: FileFilter[NpmPackage]         = create(JavaScriptExtensions)
  implicit def gemPackageFileFilter: FileFilter[GemPackage]         = create(RubyExtensions)

  def apply[A: FileFilter]: FileFilter[A] = implicitly

  def create[A](E: Extensions): FileFilter[A] = new FileFilter[A] {
    val maxFileSize: Int = 1024 * 1024
    val allowedFileNames = Set("makefile", "dockerfile", "readme", "changelog", "changes")

    def filter(file: File): Boolean = {
      val fileName = file.getName.toLowerCase
      val fileExt  = getExtension(fileName)
      (if (fileExt.isEmpty) allowedFileNames.contains(fileName)
       else E.extensions.contains(fileExt)) && file.length < maxFileSize
    }
  }
}
