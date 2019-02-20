package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.directory.СindexDirectory.RustCindex
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RustExtensions

class RustSearch extends Search {
  override protected def cindexDir: СindexDirectory = RustCindex
  override protected def extensions: Extensions     = RustExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
}
