package codesearch.core.search

import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.RubyCSearchIndex
import codesearch.core.index.repository.Extensions
import codesearch.core.index.repository.Extensions.RubyExtensions

class RubySearch extends Search {
  override protected def csearchDir: СSearchDirectory = RubyCSearchIndex
  override protected def extensions: Extensions       = RubyExtensions
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
}
