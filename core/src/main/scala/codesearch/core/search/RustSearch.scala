package codesearch.core.search

import org.slf4j.{Logger, LoggerFactory}

class RustSearch extends Searcher {

  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected def indexFile: String = ".crates_csearch_index"

  override protected def langExts: String = ".*\\.(rs)$"

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
}