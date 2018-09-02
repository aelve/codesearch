package codesearch.core.index.repository

import simulacrum.typeclass

@typeclass trait Extensions[A] {
  def extensions: Set[String]
}

object Extensions {

  implicit def jsExtensions[A <: JavaScript]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] =
      Set("js", "ts", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")
  }

  implicit def haskellExtensions[A <: Haskell]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  implicit def rubyExtensions[A <: Ruby]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = Set("rb", "rbx", "irb")
  }

  implicit def rustExtensions[A <: Rust]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = Set("rs")
  }
}
