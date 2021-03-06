package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RustExtensions

class RustSearch(val cindexDir: СindexDirectory) extends Search {
  protected def extensions: Extensions = RustExtensions
  protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
  def isTestInPath(path: String): Boolean = path.contains("/tests/")
}
