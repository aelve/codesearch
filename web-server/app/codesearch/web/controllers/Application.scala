package codesearch.web.controllers

import codesearch.core.index.{HaskellIndex, JavaScriptIndex, RubyIndex, RustIndex}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

case class LangInfo(updatedMills: Long, totalPackages: Int) {
  val updatedAgo: String = TimeAgo.using(updatedMills)
}

class Application @Inject()(
    implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index = Action.async { implicit request =>
    HaskellIndex().updated
      .zip(HaskellIndex().getSize)
      .zip(RustIndex().updated.zip(RustIndex().getSize))
      .zip(JavaScriptIndex().updated.zip(JavaScriptIndex().getSize))
      .zip(RubyIndex().updated.zip(RubyIndex().getSize))
      .map {
        case ((((updatedHackage, sizeHackage), (updatedCrates, sizeCrates)), (updatedNpm, sizeNpm)),
              (updatedGem, sizeGem)) =>
          Ok(
            views.html.index(
              LangInfo(updatedHackage.getTime, sizeHackage),
              LangInfo(updatedCrates.getTime, sizeCrates),
              LangInfo(updatedGem.getTime, sizeGem),
              LangInfo(updatedNpm.getTime, sizeNpm)
            ))
      }
  }

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }
}
