package codesearch.core.search

import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory._
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.HaskellExtensions

class HaskellSearch extends Search {
  override protected def csearchDir: СSearchDirectory = HaskellCSearchIndex
  override protected def extensions: Extensions       = HaskellExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
}
