package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, GemDB}
import codesearch.core.model.GemTable
import codesearch.core.search.RubySearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class GemSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[GemTable] {
  override def db: DefaultDB[GemTable]       = new GemDB { val db = database }
  override lazy val searchEngine: RubySearch = new RubySearch()
  override def lang: String                  = "ruby"

}
