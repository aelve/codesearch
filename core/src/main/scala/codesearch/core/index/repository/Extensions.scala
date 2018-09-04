package codesearch.core.index.repository

import simulacrum.typeclass

@typeclass trait Extensions[A] {
  def commonExtensions: Set[String] = Set("json", "cabal", "md", "txt", "xml", "yml", "yaml", "properties")
  def sourceExtensions: Set[String]
  def extensions: Set[String] = commonExtensions ++ sourceExtensions
}

object Extensions {

  implicit def jsExtensions[A <: JavaScript]: Extensions[A] = new Extensions[A] {
    override def commonExtensions: Set[String] =
      super.commonExtensions ++ Set("css", "scss", "postcss", "sass", "less", "stylus", "html", "xhtml")
    override def sourceExtensions: Set[String] = Set("js", "ts", "coffee", "jsx")
    override def extensions: Set[String]       = commonExtensions ++ sourceExtensions
  }

  implicit def haskellExtensions[A <: Haskell]: Extensions[A] = new Extensions[A] {
    override def sourceExtensions: Set[String] = Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  implicit def rubyExtensions[A <: Ruby]: Extensions[A] = new Extensions[A] {
    override def sourceExtensions: Set[String] = Set("rb", "rbx", "irb")
  }

  implicit def rustExtensions[A <: Rust]: Extensions[A] = new Extensions[A] {
    override def sourceExtensions: Set[String] = commonExtensions ++ Set("rs")
  }
}
