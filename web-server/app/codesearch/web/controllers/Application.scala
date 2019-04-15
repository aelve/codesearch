package codesearch.web.controllers

import codesearch.core.db.{CratesDB, GemDB, HackageDB, NpmDB}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, InjectedController}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

case class LangInfo(updatedMills: Long, totalPackages: Int) {
  val updatedAgo: String = TimeAgo.using(updatedMills)
}

class Application @Inject()(
    implicit val executionContext: ExecutionContext
) extends InjectedController {

  val database: Database = Database.forConfig("db")

  val HackageDB: HackageDB = new HackageDB { val db: Database = database }
  val CratesDB: CratesDB   = new CratesDB  { val db: Database = database }
  val NpmDB: NpmDB         = new NpmDB     { val db: Database = database }
  val GemDB: GemDB         = new GemDB     { val db: Database = database }

  def index: Action[AnyContent] = Action.async { implicit request =>
    HackageDB.updated
      .zip(HackageDB.getSize)
      .zip(CratesDB.updated.zip(CratesDB.getSize))
      .zip(NpmDB.updated.zip(NpmDB.getSize))
      .zip(GemDB.updated.zip(GemDB.getSize))
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
