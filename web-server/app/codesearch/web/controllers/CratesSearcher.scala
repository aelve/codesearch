package codesearch.web.controllers

import codesearch.core.db.{CratesDB, DefaultDB}
import codesearch.core.index.RustIndex
import codesearch.core.model.CratesTable
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class CratesSearcher @Inject()(implicit override val executionContext: ExecutionContext)
    extends InjectedController with BaseController[CratesTable, RustIndex] {
  override def db: DefaultDB[CratesTable] = CratesDB

  override lazy val indexEngine: RustIndex = RustIndex()

  override def lang: String = "rust"
}
