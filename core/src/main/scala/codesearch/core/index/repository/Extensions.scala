package codesearch.core.index.repository

import simulacrum.typeclass

/**
  * For more details read here:
  * @see [[https://github.com/aelve/codesearch/issues/93]]
  */
@typeclass trait Extensions[A] {

  /**
    * Defines common extensions set for each language.
    * Contained most popular text, scripts and config formats.
    * Not include source extensions.
    */
  def commonExtensions: Set[String] =
    Set(
      "json",
      "md",
      "txt",
      "xml",
      "yml",
      "yaml",
      "properties",
      "conf",
      "toml",
      "sh",
      "markdown",
      "tex",
      "c",
      "h",
      "cpp",
      "hpp"
    )

  /**
    * Defines only sources files extensions for specific language.
    */
  def sourceExtensions: Set[String]

  /**
    * Return joined extensions consisting of [[commonExtensions]] and [[sourceExtensions]] for specific language.
    */
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
    override def commonExtensions: Set[String] = super.commonExtensions ++ Set("cabal", "project")
    override def sourceExtensions: Set[String] = Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  implicit def rubyExtensions[A <: Ruby]: Extensions[A] = new Extensions[A] {
    override def commonExtensions: Set[String] = super.commonExtensions ++ Set("gemspec")
    override def sourceExtensions: Set[String] = Set("rb", "rbx", "irb")
  }

  implicit def rustExtensions[A <: Rust]: Extensions[A] = new Extensions[A] {
    override def sourceExtensions: Set[String] = Set("rs")
  }
}
