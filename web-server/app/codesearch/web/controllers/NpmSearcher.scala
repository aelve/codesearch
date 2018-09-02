package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, NpmDB}
import codesearch.core.index.JavaScriptIndex
import codesearch.core.model.NpmTable
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class NpmSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with BaseController[NpmTable, JavaScriptIndex] {
  override def db: DefaultDB[NpmTable] = NpmDB

  override lazy val indexEngine: JavaScriptIndex = JavaScriptIndex()

  override def lang: String = "js"
}
