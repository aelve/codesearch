package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, HackageDB}
import codesearch.core.model.HackageTable
import codesearch.core.search.HaskellSearchRequest
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[HackageTable, HaskellSearchRequest] {
  override def db: DefaultDB[HackageTable]     = HackageDB
  override lazy val indexEngine: HaskellSearchRequest = new HaskellSearchRequest()
  override def lang: String                    = "haskell"
}
