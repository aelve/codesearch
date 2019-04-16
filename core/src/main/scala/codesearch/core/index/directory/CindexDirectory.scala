package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.syntax.path._

trait СindexDirectory {

  /** Defined main path to index */
  implicit val root: Path

  /** Defines package repository name */
  def packageRepository: String

  /** Function returns index directory in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def indexDirAs[O](implicit D: DirAs[O]): O = D.dir(packageRepository)

  /** Function returns temporary index directory in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def tempIndexDirAs[O](implicit D: DirAs[O]): O = D.tempDir(packageRepository)

  /** Function returns path to file that contains directories to index in representation depending from type parameter
    *
    * @param D is implicit instance of [[DirAs]] trait
    * @tparam O is return type
    */
  def dirsToIndex[O](implicit D: DirAs[O]): O = D.dirsToIndex(packageRepository)
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
  def dir(packageManager: String): A
  def tempDir(packageManager: String): A
  def dirsToIndex(packageManager: String): A
}

object DirAs {
  implicit def asString(implicit root: Path): DirAs[String] = new DirAs[String] {
    private def fullPath(relativePath: Path): String         = relativePath.toFile.getCanonicalPath
    override def dirsToIndex(packageManager: String): String = s"${asPath.dirsToIndex(packageManager)}"
    override def dir(packageManager: String): String         = fullPath(asPath.dir(packageManager))
    override def tempDir(packageManager: String): String     = fullPath(asPath.tempDir(packageManager))
  }

  implicit def asPath(implicit root: Path): DirAs[Path] = new DirAs[Path] {
    private def index(packageManager: String): String      = s".${packageManager}_csearch_index"
    override def dirsToIndex(packageManager: String): Path = root / s".${packageManager}_dirs_for_index"
    override def dir(packageManager: String): Path         = root / index(packageManager)
    override def tempDir(packageManager: String): Path     = root / s"${index(packageManager)}.tmp"
  }
}
