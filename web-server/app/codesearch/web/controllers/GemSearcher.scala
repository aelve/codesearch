package codesearch.web.controllers

import codesearch.core.db.{DefaultDB, GemDB}
import codesearch.core.index.RubyIndex
import codesearch.core.model.GemTable
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class GemSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[GemTable, RubyIndex] {

  override def db: DefaultDB[GemTable] = GemDB

  override lazy val indexEngine: RubyIndex = RubyIndex()

  override def lang: String = "ruby"

}
