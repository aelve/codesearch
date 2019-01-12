package codesearch.core.index.repository

/**
  * For more details read here:
  * @see [[https://github.com/aelve/codesearch/issues/93]]
  */
trait Extensions {

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

  final object JavaScriptExtensions extends Extensions {
    override def commonExtensions: Set[String] =
      super.commonExtensions ++ Set("css", "scss", "postcss", "sass", "less", "stylus", "html", "xhtml")
    def sourceExtensions: Set[String] = Set("js", "ts", "coffee", "jsx")
  }

  final object HaskellExtensions extends Extensions {
    override def commonExtensions: Set[String] = super.commonExtensions ++ Set("cabal", "project")
    def sourceExtensions: Set[String] = Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  final object RubyExtensions extends Extensions {
    override def commonExtensions: Set[String] = super.commonExtensions ++ Set("gemspec")
    def sourceExtensions: Set[String] = Set("rb", "rbx", "irb")
  }

  final object RustExtensions extends Extensions {
    def sourceExtensions: Set[String] = Set("rs")
  }
}
