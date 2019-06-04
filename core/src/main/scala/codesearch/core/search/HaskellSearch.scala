package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.directory.СindexDirectory._
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.HaskellExtensions

class HaskellSearch extends Search {
  override protected def cindexDir: СindexDirectory = HaskellCindex
  override protected def extensions: Extensions     = HaskellExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
}
