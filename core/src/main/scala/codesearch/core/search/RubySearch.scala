package codesearch.core.search

import codesearch.core.index.Ruby
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions

class RubySearch extends Search {
  override protected type Tag = Ruby
  override protected def csearchDir: СSearchDirectory[Tag] = implicitly
  override protected def extensions: Extensions[Tag]       = implicitly
  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
}
