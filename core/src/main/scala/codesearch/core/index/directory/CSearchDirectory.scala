package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.{Haskell, JavaScript, Ruby, Rust}
import codesearch.core.index.directory.Preamble._
import simulacrum.typeclass

@typeclass trait СSearchDirectory[A] {

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
  implicit def haskellCSearchIndex[A <: Haskell]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageRepository: String = "hackage"
  }

  implicit def javaScriptCSearchIndex[A <: JavaScript]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageRepository: String = "npm"
  }

  implicit def rubyCSearchIndex[A <: Ruby]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageRepository: String = "gem"
  }

  implicit def rustCSearchIndex[A <: Rust]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageRepository: String = "crates"
  }
}

@typeclass trait DirAs[A] {
  def dir(packageManager: String): A
  def tempDir(packageManager: String): A
}

object DirAs {

  private val root: Path = Paths.get("./index/csearch/")

  implicit def asString: DirAs[String] = new DirAs[String] {
    override def dir(packageManager: String): String     = asPath.dir(packageManager).toFile.getCanonicalPath
    override def tempDir(packageManager: String): String = asPath.tempDir(packageManager).toFile.getAbsolutePath
  }

  implicit def asPath: DirAs[Path] = new DirAs[Path] {
    private def index(packageManager: String): String  = s".${packageManager}_csearch_index"
    override def dir(packageManager: String): Path     = root / index(packageManager)
    override def tempDir(packageManager: String): Path = root / s"${index(packageManager)}.tmp"
  }
}
