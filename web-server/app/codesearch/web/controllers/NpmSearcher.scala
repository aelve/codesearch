package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{DefaultDB, NpmDB}
import codesearch.core.index.directory.JavaScriptCindex
import codesearch.core.model.NpmTable
import codesearch.core.search.JavaScriptSearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class NpmSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[NpmTable] {
  override def db: DefaultDB[NpmTable] = new NpmDB { val db = database }
  override lazy val searchEngine: JavaScriptSearch = new JavaScriptSearch(
    JavaScriptCindex(Paths.get("./index/cindex/")))
  override def lang: String = "js"
}
