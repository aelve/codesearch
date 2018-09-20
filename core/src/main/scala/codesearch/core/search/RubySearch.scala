package codesearch.core.search
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class RubySearch(implicit ec: ExecutionContext) extends Searcher {

  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected def indexFile: String = ".gem_csearch_index"

  override protected def langExts: String = ".*\\.(rb)$"

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://rubygems.org/gems/$packageName/versions/$version"
}
