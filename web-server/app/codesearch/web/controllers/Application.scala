package codesearch.web.controllers

import javax.inject.{Inject, Named}
import play.api.mvc.{Action, InjectedController}

import scala.concurrent.ExecutionContext

class Application @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def haskell = Action.async { implicit request =>
    HackageDB.updated.map(updated => Ok(views.html.haskell(updated)))
  }

  def index = Action.async { implicit request =>
    HackageDB.updated.map(updated => Ok(views.html.index(updated)))
  }

}
