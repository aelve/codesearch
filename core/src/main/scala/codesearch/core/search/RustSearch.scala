package codesearch.core.search

import codesearch.core.index.Rust
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions

class RustSearch extends Search {
  override protected type Tag = Rust
  override protected def csearchDir: СSearchDirectory[Tag] = implicitly
  override protected def extensions: Extensions[Tag]       = implicitly
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
}
