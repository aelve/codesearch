package codesearch.web.controllers

import codesearch.core.db.HackageDB
import codesearch.core.index.HaskellIndex
import codesearch.core.index.LanguageIndex.{CSearchPage, SearchArguments}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) = Action.async { implicit request =>
    val callURI = s"/haskell/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

    HackageDB.updated.flatMap(updated =>
      HaskellIndex().csearch(SearchArguments(query, insensitive == "on", precise == "on", sources == "on"), page.toInt) map {
        case CSearchPage(results, count) =>
          Ok(views.html.search(
            TimeAgo.using(updated.getTime),
            results,
            query,
            insensitive == "on",
            precise == "on",
            sources == "on",
            page = page.toInt,
            count,
            callURI
          ))
      }
    )
  }
}
