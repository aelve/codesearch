package codesearch.web.controllers

import cats.data.OptionT
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import com.github.marlonlom.utilities.timeago.TimeAgo
import play.api.mvc.{Action, AnyContent, InjectedController}
import cats.instances.future._
import codesearch.core.config.{Config, SnippetConfig}
import codesearch.core.search.Searcher
import codesearch.core.search.Searcher.{CSearchPage, SearchArguments}
import codesearch.web.controllers.SearchController._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait SearchController[V <: DefaultTable, I <: Searcher] { self: InjectedController =>

  implicit val executionContext: ExecutionContext
  def db: DefaultDB[V]
  def indexEngine: I
  def lang: String

  def index: Action[AnyContent] = Action.async { implicit request =>
    db.updated.map(
      updated =>
        Ok(
          views.html.search(
            updated = TimeAgo.using(updated.getTime),
            lang = lang
          )))
  }

  def search(query: String, insensitive: String, precise: String, sources: String, page: String): Action[AnyContent] =
    Action.async { implicit request =>
      val callURI = s"/$lang/search?query=$query&insensitive=$insensitive&precise=$precise&sources=$sources"

      db.updated.flatMap(
        updated =>
          indexEngine.search(SearchArguments(query = query,
                                             insensitive = insensitive == "on",
                                             preciseMatch = precise == "on",
                                             sourcesOnly = sources == "on"),
                             page.toInt) map {
            case CSearchPage(results, total) =>
              Ok(
                views.html.search_results(
                  updated = TimeAgo.using(updated.getTime),
                  packages = results,
                  query = query,
                  insensitive = insensitive == "on",
                  precise = precise == "on",
                  sources = sources == "on",
                  page = page.toInt,
                  totalMatches = total,
                  callURI = callURI,
                  lang = lang
                ))
        })
    }

  def source(relativePath: String): Action[AnyContent] = Action.async { implicit request =>
    val realPath = s"data/$relativePath"
    OptionT
      .fromOption[Future](indexEngine.packageName(realPath))
      .flatMap(pack => {
        OptionT(Helper.readFileAsync(realPath)).map(s => (pack, s))
      })
      .map {
        case (pack, code) =>
          Ok(
            views.html.sourceCode(sourceCode = code,
                                  pack = pack,
                                  relativePath = relativePath.split('/').drop(3).mkString("/"),
                                  lang = lang))
      }
      .getOrElse(NotFound.apply("Not found"))
  }

}

object SearchController {
  lazy implicit val snippetConfig: SnippetConfig = Config
    .load()
    .toOption
    .map(_.snippetConfig)
    .getOrElse(SnippetConfig(30, 3, 5))
}
