package codesearch.core.search

import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RubyExtensions

class RubySearch(val cindexDir: СindexDirectory) extends Search {
  override protected def extensions: Extensions = RubyExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
}
