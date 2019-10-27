package codesearch.web.controllers

import codesearch.core.db.{CratesDB, HackageDB}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, InjectedController}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

final case class LangInfo(
    updatedMills: Long,
    totalPackages: Int
) { val updatedAgo: String = TimeAgo.using(updatedMills) }

class Application @Inject()(
    implicit val executionContext: ExecutionContext
) extends InjectedController {

  private val HackageDB: HackageDB = new HackageDB { val db: Database = Application.database }
  private val CratesDB: CratesDB   = new CratesDB  { val db: Database = Application.database }

  def index: Action[AnyContent] = Action.async { implicit request =>
    HackageDB.updated
      .zip(HackageDB.getSize)
      .zip(CratesDB.updated.zip(CratesDB.getSize))
      .map {
        case ((updatedHackage, sizeHackage), (updatedCrates, sizeCrates)) =>
          Ok(
            views.html.index(
              LangInfo(updatedHackage.getTime, sizeHackage),
              LangInfo(updatedCrates.getTime, sizeCrates)
            )
          )
      }
  }

  def untrail(path: String): Action[AnyContent] = Action {
    MovedPermanently("/" + path)
  }
}

private object Application {
  val database = Database.forConfig("db")
}
