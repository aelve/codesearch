package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{DefaultDB, HackageDB}
import codesearch.core.index.directory.HaskellCindex
import codesearch.core.model.HackageTable
import codesearch.core.search.HaskellSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

final class HackageSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[HackageTable] {
  val db: DefaultDB[HackageTable]      = HackageDBImpl
  lazy val searchEngine: HaskellSearch = new HaskellSearch(HaskellCindex(Paths.get("./index/cindex/")))
  val lang: String                     = "haskell"
}

private object HackageDBImpl extends HackageDB {
  val db = Application.database
}