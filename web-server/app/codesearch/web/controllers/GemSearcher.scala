package codesearch.web.controllers

import java.nio.file.Paths

import codesearch.core.db.{DefaultDB, GemDB}
import codesearch.core.index.directory.RubyCindex
import codesearch.core.model.GemTable
import codesearch.core.search.RubySearch
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

final class GemSearcher @Inject()(
    implicit override val executionContext: ExecutionContext
) extends InjectedController with SearchController[GemTable] {
  val db: DefaultDB[GemTable]       = GemDBImpl
  lazy val searchEngine: RubySearch = new RubySearch(RubyCindex(Paths.get("./index/cindex/")))
  val lang: String                  = "ruby"
}

private object GemDBImpl extends GemDB {
  val db = Application.database
}
