package codesearch.web.controllers

import codesearch.core.db.{CratesDB, DefaultDB}
import codesearch.core.model.CratesTable
import codesearch.core.search.RustSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class CratesSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[CratesTable] {
  override def db: DefaultDB[CratesTable]    = CratesDB
  override lazy val searchEngine: RustSearch = new RustSearch()
  override def lang: String                  = "rust"
}
