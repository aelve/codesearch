package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.directory.Preamble._
import codesearch.core.index.directory.СSearchDirectory.root

trait СSearchDirectory {

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
}

object СSearchDirectory {
  private[index] val root: Path = Paths.get("./index/csearch/")

  final object HaskellCSearchIndex extends СSearchDirectory {
    def packageRepository: String = "hackage"
  }

  final object JavaScriptCSearchIndex extends СSearchDirectory {
    def packageRepository: String = "npm"
  }

  final object RubyCSearchIndex extends СSearchDirectory {
    def packageRepository: String = "gem"
  }

  final object RustCSearchIndex extends СSearchDirectory {
    def packageRepository: String = "crates"
  }
}

trait DirAs[A] {
  def dir(packageManager: String): A
  def tempDir(packageManager: String): A
}

object DirAs {
  implicit def asString: DirAs[String] = new DirAs[String] {
    private def fullPath(relativePath: Path): String     = relativePath.toFile.getCanonicalPath
    override def dir(packageManager: String): String     = fullPath(asPath.dir(packageManager))
    override def tempDir(packageManager: String): String = fullPath(asPath.tempDir(packageManager))
  }

  implicit def asPath: DirAs[Path] = new DirAs[Path] {
    private def index(packageManager: String): String  = s".${packageManager}_csearch_index"
    override def dir(packageManager: String): Path     = root / index(packageManager)
    override def tempDir(packageManager: String): Path = root / s"${index(packageManager)}.tmp"
  }
}
