package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{CratesDB, DefaultDB}
import codesearch.core.index.directory.RustCindex
import codesearch.core.model.CratesTable
import codesearch.core.search.RustSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class CratesSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[CratesTable] {
  override def db: DefaultDB[CratesTable]    = new CratesDB { val db = database }
  override lazy val searchEngine: RustSearch = new RustSearch(RustCindex(Paths.get("./index/cindex/")))
  override def lang: String                  = "rust"
}
