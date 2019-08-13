package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.JavaScriptExtensions

class JavaScriptSearch(val cindexDir: СindexDirectory) extends Search {
  override protected def extensions: Extensions = JavaScriptExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://www.npmjs.com/package/$packageName/v/$version"
  def isTestInPath(path: String): Boolean = path.matches(".*((?i)(test|spec|tests)).*")
}
