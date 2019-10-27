package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{CratesDB, DefaultDB}
import codesearch.core.index.directory.RustCindex
import codesearch.core.model.CratesTable
import codesearch.core.search.RustSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

final class CratesSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[CratesTable] {
  val db: DefaultDB[CratesTable]    = CratesDBImpl
  lazy val searchEngine: RustSearch = new RustSearch(RustCindex(Paths.get("./index/cindex/")))
  val lang: String                  = "rust"
}

private object CratesDBImpl extends CratesDB {
  val db = Application.database
}
