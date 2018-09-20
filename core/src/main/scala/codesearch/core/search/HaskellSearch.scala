package codesearch.core.search
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.ExecutionContext

class HaskellSearch(implicit ec: ExecutionContext) extends Searcher {

  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected def langExts: String = ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$"

  override protected def indexFile: String = ".hackage_csearch_index"

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"
}
