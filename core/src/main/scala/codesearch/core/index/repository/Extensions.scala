package codesearch.core.index.repository

import simulacrum.typeclass

@typeclass trait Extensions[A] {
  def commonExtensions: Set[String] =
    Set("json", "cabal", "md", "txt", "xml", "yml", "yaml", "properties")
  def extensions: Set[String]
}

object Extensions {

  implicit def jsExtensions[A <: JavaScript]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = commonExtensions ++ Set("js", "ts", "json", "coffee")
  }

  implicit def haskellExtensions[A <: Haskell]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = commonExtensions ++ Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  implicit def rubyExtensions[A <: Ruby]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = commonExtensions ++ Set("rb", "rbx", "irb")
  }

  implicit def rustExtensions[A <: Rust]: Extensions[A] = new Extensions[A] {
    override def extensions: Set[String] = commonExtensions ++ Set("rs")
  }
}