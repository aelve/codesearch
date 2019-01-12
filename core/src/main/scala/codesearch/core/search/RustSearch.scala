package codesearch.core.search

import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.RustCSearchIndex
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RustExtensions

class RustSearch extends Search {
  override protected def csearchDir: СSearchDirectory = RustCSearchIndex
  override protected def extensions: Extensions       = RustExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
}
