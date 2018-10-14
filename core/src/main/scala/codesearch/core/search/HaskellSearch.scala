package codesearch.core.search

import codesearch.core.index.Haskell
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory._
import codesearch.core.index.repository.Extensions

class HaskellSearch extends Search {
  override protected type Tag = Haskell
  override protected def csearchDir: СSearchDirectory[Tag] = implicitly
  override protected def extensions: Extensions[Tag]       = implicitly
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
}
