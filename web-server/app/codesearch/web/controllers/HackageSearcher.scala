package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, HackageDB}
import codesearch.core.model.HackageTable
import codesearch.core.search.HaskellSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[HackageTable, HaskellSearch] {
  override def db: DefaultDB[HackageTable]     = HackageDB
  override lazy val indexEngine: HaskellSearch = new HaskellSearch()
  override def lang: String                    = "haskell"
}
