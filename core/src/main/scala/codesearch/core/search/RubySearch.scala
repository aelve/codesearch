package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RubyExtensions

class RubySearch(val cindexDir: СindexDirectory) extends Search {
  protected def extensions: Extensions = RubyExtensions
  protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
  def isTestInPath(path: String): Boolean = path.contains("/spec/") || path.contains("/test/")
}
