package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, HackageDB}
import codesearch.core.index.HaskellIndex
import codesearch.core.model.HackageTable
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[HackageTable, HaskellIndex] {
  override def db: DefaultDB[HackageTable] = HackageDB

  override lazy val indexEngine: HaskellIndex = HaskellIndex()

  override def lang: String = "haskell"
}
