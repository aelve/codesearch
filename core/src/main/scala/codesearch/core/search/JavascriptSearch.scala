package codesearch.core.search
import codesearch.core.index.repository.Extensions
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class JavascriptSearch(implicit ec: ExecutionContext) extends Searcher {

  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected def langExts: String = ".*\\.(js|json)$"

  override protected def indexFile: String = ".npm_csearch_index"

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
}
