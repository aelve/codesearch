package codesearch.core.index.repository

trait Extension[A] {
  def extensions: Set[String]
}

object Extensions {

  implicit def jsExtensions[A <: JavaScript]: Extension[A] = new Extension[A] {
    override def extensions: Set[String] =
      Set("js", "ts", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")
  }

  implicit def haskellExtensions[A <: Haskell]: Extension[A] = new Extension[A] {
    override def extensions: Set[String] = Set("hs", "lhs", "hsc", "hs-boot", "lhs-boot")
  }

  implicit def rubyExtensions[A <: Ruby]: Extension[A] = new Extension[A] {
    override def extensions: Set[String] = Set("rb", "rbx", "irb")
  }

  implicit def rustExtensions[A <: Rust]: Extension[A] = new Extension[A] {
    override def extensions: Set[String] = Set("rs", "cabal")
  }

  object Extension {
    def apply[A <: SourcePackage](implicit ext: Extension[A]): Extension[A] = ext
  }
}
