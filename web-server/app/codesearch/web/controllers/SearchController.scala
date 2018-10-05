package codesearch.web.controllers

import cats.data.OptionT
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import com.github.marlonlom.utilities.timeago.TimeAgo
import play.api.mvc.{Action, AnyContent, InjectedController}
import cats.instances.future._
import cats.instances.string._
import cats.syntax.eq._
import codesearch.core.config.{Config, SnippetConfig}
import codesearch.core.search.{JsSearchRequest, S, Searcher}
import codesearch.core.search.Searcher.{CSearchPage, SearchArguments}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait SearchController[V <: DefaultTable, I <: S] { self: InjectedController =>

  implicit val executionContext: ExecutionContext
  def db: DefaultDB[V]
  def searchEngine: I
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
      db.updated.flatMap(updated =>
        searchEngine.search(query, isEnabled(insensitive), isEnabled(precise), isEnabled(sources), page.toInt) map {
          case CSearchPage(results, total) =>
            Ok(
              views.html.search_results(
                updated = TimeAgo.using(updated.getTime),
                packages = results,
                query = query,
                insensitive = isEnabled(insensitive),
                precise = isEnabled(precise),
                sources = isEnabled(sources),
                page = page.toInt,
                totalMatches = total,
                callURI = callURI,
                lang = lang
              ))
        } unsafeToFuture)
    }

  def source(relativePath: String, query: String, L: Int): Action[AnyContent] =
    Action.async { implicit request =>
      val realPath = s"data/$relativePath"
      OptionT
        .fromOption[Future](searchEngine.packageName(realPath))
        .flatMap(pack => {
          OptionT.liftF(Helper.readFileAsync(realPath).unsafeToFuture()).map(s => (pack, s))
        })
        .map {
          case (pack, code) =>
            Ok(
              views.html.sourceCode(sourceCode = code,
                                    pack = pack,
                                    relativePath = relativePath.split('/').drop(3).mkString("/"),
                                    lang = lang,
                                    query = query,
                                    firstMatch = L))
        }
        .getOrElse(NotFound.apply("Not found"))
    }

  private def isEnabled(param: String) = param === "on"
}

object SearchController {
  lazy implicit val snippetConfig: SnippetConfig = Config.load
    .map(_.snippetConfig)
    .unsafeRunSync() //TODO: pass config here as parameter
}
