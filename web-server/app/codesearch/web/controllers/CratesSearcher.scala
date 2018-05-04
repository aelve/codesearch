package codesearch.web.controllers

import codesearch.core.db.CratesDB
import codesearch.core.index.CratesSources
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class CratesSearcher @Inject() (implicit val executionContext: ExecutionContext
                               ) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String) = Action.async { implicit request =>
    CratesDB.updated.map(updated =>
      Ok(views.html.rust_search(updated,
        CratesSources.csearch(query, insensitive == "on", precise == "on", sources == "on"),
        query,
        insensitive == "on",
        precise == "on",
        sources == "on"
      ))
    )
  }
}
