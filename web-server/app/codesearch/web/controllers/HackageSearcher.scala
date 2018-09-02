package codesearch.web.controllers

import codesearch.core.db.HackageDB
import codesearch.core.index.HaskellIndex
import codesearch.core.index.LanguageIndex.{CSearchPage, SearchArguments}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class HackageSearcher @Inject()(
    implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) = Action.async {
    implicit request =>
      val callURI = s"/haskell/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

      HackageDB.updated.flatMap(
        updated =>
          HaskellIndex().search(SearchArguments(query = query,
                                                insensitive = insensitive == "on",
                                                preciseMatch = precise == "on",
                                                sourcesOnly = sources == "on"),
                                page.toInt) map {
            case CSearchPage(results, total) =>
              Ok(
                views.html.search(
                  updated = TimeAgo.using(updated.getTime),
                  packages = results,
                  query = query,
                  insensitive = insensitive == "on",
                  precise = precise == "on",
                  sources = sources == "on",
                  page = page.toInt,
                  totalMatches = total,
                  callURI = callURI
                ))
        })
  }
}
