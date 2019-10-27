package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{DefaultDB, NpmDB}
import codesearch.core.index.directory.JavaScriptCindex
import codesearch.core.model.NpmTable
import codesearch.core.search.JavaScriptSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

final class NpmSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[NpmTable] {
  val db: DefaultDB[NpmTable]             = NpmDBImpl
  lazy val searchEngine: JavaScriptSearch = new JavaScriptSearch(JavaScriptCindex(Paths.get("./index/cindex/")))
  val lang: String                        = "js"
}

private object NpmDBImpl extends NpmDB {
  val db = Application.database
}
