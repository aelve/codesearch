package codesearch.core.search

import codesearch.core.index.Haskell
import org.slf4j.{Logger, LoggerFactory}

class HaskellSearch extends Haskell {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected def langExts: String = ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$"

  protected def indexFile: String = ".hackage_csearch_index"

  protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
}
