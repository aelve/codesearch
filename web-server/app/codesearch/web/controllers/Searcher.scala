package codesearch.web.controllers

import javax.inject.{Inject, Named}
import play.api.mvc.{Action, InjectedController}

import scala.concurrent.ExecutionContext

import codesearch.core.Main

class Searcher @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String) = Action { implicit request =>
    Ok(views.html.search(Main.csearch(query)))
  }
}
