package codesearch.web.controllers

import codesearch.core.db.HackageDB
import codesearch.core.index.HackageSources
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) = Action.async { implicit request =>
    val callURI = s"/haskell/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

    HackageDB.updated.map(updated =>
      Ok(views.html.search(
        TimeAgo.using(updated.getTime),
        HackageSources.csearch(query, insensitive == "on", precise == "on", sources == "on", page.toInt),
        query,
        insensitive == "on",
        precise == "on",
        sources == "on",
        page = page.toInt,
        callURI
      ))
    )
  }
}
