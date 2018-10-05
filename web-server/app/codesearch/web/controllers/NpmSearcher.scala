package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, NpmDB}
import codesearch.core.model.NpmTable
import codesearch.core.search.JavascriptSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class NpmSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[NpmTable, JavascriptSearch] {
  override def db: DefaultDB[NpmTable]            = NpmDB
  override lazy val searchEngine: JavascriptSearch = new JavascriptSearch()
  override def lang: String                       = "js"
}
