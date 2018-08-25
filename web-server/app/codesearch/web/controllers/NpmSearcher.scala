package codesearch.web.controllers

import codesearch.core.db.NpmDB
import codesearch.core.index.JavaScriptIndex
import codesearch.core.index.LanguageIndex.SearchArguments
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class NpmSearcher @Inject() (
                                  implicit val executionContext: ExecutionContext
                                ) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) = Action.async { implicit request =>
    val callURI = s"/js/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

    NpmDB.updated.map(updated =>
      JavaScriptIndex().csearch(SearchArguments(query, insensitive == "on", precise == "on", sources == "on"), page.toInt) match {
        case (count, results) =>
          Ok(views.html.javascript_search(
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
