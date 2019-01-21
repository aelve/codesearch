package codesearch.core.search

import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.JavaScriptCSearchIndex
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.JavaScriptExtensions

class JavaScriptSearch extends Search {
  override protected def csearchDir: СSearchDirectory = JavaScriptCSearchIndex
  override protected def extensions: Extensions       = JavaScriptExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://www.npmjs.com/package/$packageName/v/$version"
}
