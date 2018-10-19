package codesearch.core.search

import codesearch.core.index.JavaScript
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions

class JavaScriptSearch extends Search {
  override protected type Tag = JavaScript
  override protected def csearchDir: СSearchDirectory[Tag] = implicitly
  override protected def extensions: Extensions[Tag]       = implicitly
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://www.npmjs.com/package/$packageName/v/$version"
}
