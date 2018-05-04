package codesearch.web.controllers

import codesearch.core.index.{CratesIndex, HackageIndex}
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class Application @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def haskell = Action.async { implicit request =>
    HackageIndex.updated.map(updated => Ok(views.html.haskell(updated)))
  }

  def rust = Action.async { implicit request =>
    CratesIndex.updated.map(updated => Ok(views.html.rust(updated)))
  }

  def index = Action.async { implicit request =>
    HackageIndex.updated.map(updated => Ok(views.html.index(updated)))
  }

}
