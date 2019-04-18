package codesearch.core.index.directory

import java.nio.file.Path

import codesearch.core.syntax.path._

trait СindexDirectory {

  /** Defined main path to index */
  def root: Path

  /** Defines package repository name */
  def packageRepository: String

  /** Function returns index directory in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def indexDirAs[O](implicit D: DirAs[O]): O = D.dir(packageRepository, root)

  /** Function returns temporary index directory in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def tempIndexDirAs[O](implicit D: DirAs[O]): O = D.tempDir(packageRepository, root)

  /** Function returns path to file that contains directories to index in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def dirsToIndex[O](implicit D: DirAs[O]): O = D.dirsToIndex(packageRepository, root)
}

case class HaskellCindex(root: Path) extends СindexDirectory {
  def packageRepository: String = "hackage"
}

case class JavaScriptCindex(root: Path) extends СindexDirectory {
  def packageRepository: String = "npm"
}

case class RubyCindex(root: Path) extends СindexDirectory {
  def packageRepository: String = "gem"
}

case class RustCindex(root: Path) extends СindexDirectory {
  def packageRepository: String = "crates"
}

trait DirAs[A] {
  def dir(packageManager: String, root: Path): A
  def tempDir(packageManager: String, root: Path): A
  def dirsToIndex(packageManager: String, root: Path): A
}

object DirAs {
  implicit def asString(root: Path): DirAs[String] = new DirAs[String] {
    private def fullPath(relativePath: Path): String         = relativePath.toFile.getCanonicalPath
    override def dirsToIndex(packageManager: String, root: Path): String = s"${asPath.dirsToIndex(packageManager, root)}"
    override def dir(packageManager: String, root: Path): String         = fullPath(asPath.dir(packageManager, root))
    override def tempDir(packageManager: String, root: Path): String     = fullPath(asPath.tempDir(packageManager, root))
  }

  implicit def asPath: DirAs[Path] = new DirAs[Path] {
    private def index(packageManager: String, root: Path): String      = s".${packageManager}_csearch_index"
    override def dirsToIndex(packageManager: String, root: Path): Path = root / s".${packageManager}_dirs_for_index"
    override def dir(packageManager: String, root: Path): Path         = root / index(packageManager, root)
    override def tempDir(packageManager: String, root: Path): Path     = root / s"${index(packageManager, root)}.tmp"
  }
}