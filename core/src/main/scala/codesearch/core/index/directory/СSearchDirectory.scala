package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.{Haskell, JavaScript, Ruby, Rust}
import codesearch.core.index.directory.PathOps._
import simulacrum.typeclass

@typeclass trait СSearchDirectory[A] {
  def packageManager: String
  def indexDirAs[O](implicit D: DirAs[O]): O = D.dir(packageManager)
  def tempIndexDirAs[O](implicit D: DirAs[O]): O = D.tempDir(packageManager)
}

object СSearchDirectory {

  implicit def haskellCSearchIndex[A <: Haskell]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageManager: String = "hackage"
  }

  implicit def javaScriptCSearchIndex[A <: JavaScript]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageManager: String = "npm"
  }

  implicit def rubyCSearchIndex[A <: Ruby]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageManager: String = "gem"
  }

  implicit def rustCSearchIndex[A <: Rust]: СSearchDirectory[A] = new СSearchDirectory[A] {
    override def packageManager: String = "crates"
  }
}

@typeclass trait DirAs[A] {
  def dir(packageManager: String): A
  def tempDir(packageManager: String): A
}

object DirAs {
  implicit def asString: DirAs[String] = new DirAs[String] {
    override def dir(packageManager: String): String     = asPath.dir(packageManager).toFile.getCanonicalPath
    override def tempDir(packageManager: String): String = asPath.tempDir(packageManager).toFile.getAbsolutePath
  }

  implicit def asPath: DirAs[Path] = new DirAs[Path] {
    private def root: Path                             = Paths.get(s"./index/csearch/")
    private def index(packageManager: String): String  = s".${packageManager}_csearch_index"
    override def dir(packageManager: String): Path     = root / index(packageManager)
    override def tempDir(packageManager: String): Path = root / s"${index(packageManager)}.tmp"
  }
}
