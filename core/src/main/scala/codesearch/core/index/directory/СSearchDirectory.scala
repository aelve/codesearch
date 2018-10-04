package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.{Haskell, JavaScript, Ruby, Rust}
import simulacrum.typeclass

@typeclass trait СSearchDirectory[A] {
  def packageManager: String
  def indexDirAs[O: DirAs]: O = DirAs[O].dir(packageManager)
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
}

object DirAs {
  implicit def asString: DirAs[String] =
    (packageManager: String) => DirAs[Path].dir(packageManager).toFile.getCanonicalPath

  implicit def asPath: DirAs[Path] =
    (packageManager: String) => Paths.get(s"./index/csearch/.${packageManager}_csearch_index")
}
