package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.HaskellExtensions

class HaskellSearch(val cindexDir: СindexDirectory) extends Search {
  override protected def extensions: Extensions = HaskellExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
  def isTestInPath(path: String): Boolean =
    path.matches(".*/((?i)(test|tests|testsuite|testsuites|test-suite|test-suites))/.*")
}
