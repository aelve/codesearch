package codesearch.web.controllers

import javax.inject.{Inject, Named}
import play.api.mvc.{Action, InjectedController}

import scala.concurrent.ExecutionContext
import codesearch.core.index.IndexerUtility

class Searcher @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String) = Action.async { implicit request =>
    HackageDB.updated.map(updated =>
      Ok(views.html.search(updated,
        IndexerUtility.csearch(query, insensitive == "on", precise == "on", sources == "on"),
        query,
        insensitive == "on",
        precise == "on",
        sources == "on"
      ))
    )
  }
}
