package codesearch.web.controllers

import codesearch.core.db.CratesDB
import codesearch.core.index.LanguageIndex.{CSearchPage, SearchArguments}
import codesearch.core.index.RustIndex
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class CratesSearcher @Inject()(implicit val executionContext: ExecutionContext) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) = Action.async {
    implicit request =>
      val callURI = s"/rust/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

      CratesDB.updated.flatMap(
        updated =>
          RustIndex()
            .csearch(SearchArguments(query, insensitive == "on", precise == "on", sources == "on"), page.toInt)
            .map {
              case CSearchPage(results, count) =>
                Ok(
                  views.html.rust_search(
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
          })
  }
}
