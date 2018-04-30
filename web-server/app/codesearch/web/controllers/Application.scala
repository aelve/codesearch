package codesearch.web.controllers

import javax.inject.{Inject, Named}
import play.api.mvc.{Action, InjectedController}

import scala.concurrent.ExecutionContext

class Application @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index_haskell = Action { implicit request =>
    Ok(views.html.index_haskell())
  }

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

}
