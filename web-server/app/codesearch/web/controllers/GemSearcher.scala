package codesearch.web.controllers

import codesearch.core.db.GemDB
import codesearch.core.index.RubyIndex
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

class GemSearcher @Inject()(
    implicit val executionContext: ExecutionContext
) extends InjectedController {

  def index(query: String, insensitive: String, precise: String, sources: String, page: String) =
    Action.async { implicit request =>
      val callURI = s"/ruby/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

      GemDB.updated.map(updated =>
        RubyIndex().csearch(query, insensitive == "on", precise == "on", sources == "on", page.toInt) match {
          case (count, results) =>
            Ok(
              views.html.ruby_search(
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
