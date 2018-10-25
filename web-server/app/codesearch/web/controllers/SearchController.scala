package codesearch.web.controllers

import cats.data.OptionT
import cats.instances.future._
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.search.Search.CSearchPage
import codesearch.core.search.{Search, SearchRequest}
import codesearch.core.util.Helper
import com.github.marlonlom.utilities.timeago.TimeAgo
import play.api.mvc.{Action, AnyContent, InjectedController}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait SearchController[V <: DefaultTable] { self: InjectedController =>

  implicit val executionContext: ExecutionContext
  def db: DefaultDB[V]
  def searchEngine: Search
  def lang: String

  def index: Action[AnyContent] = Action.async { implicit request =>
    db.updated.map(
      updated =>
        Ok(
          views.html.search(
            updated = TimeAgo.using(updated.getTime),
            lang = lang
          )
      )
    )
  }

  def search(
      query: String,
      insensitive: String,
      precise: String,
      sources: String,
      linesBefore: String,
      linesAfter: String,
      page: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      val request = SearchRequest.applyRaw(query, insensitive, precise, sources, linesBefore, linesAfter, page)
      db.updated.flatMap { updated =>
        searchEngine.search(request) map {
          case CSearchPage(results, total) =>
            Ok(
              views.html.search_results(
                updated = TimeAgo.using(updated.getTime),
                packages = results,
                query = query,
                insensitive = request.insensitive,
                precise = request.preciseMatch,
                sources = request.sourcesOnly,
                linesBefore = request.linesBefore,
                linesAfter = request.linesAfter,
                page = request.page,
                totalMatches = total,
                callUri = callUri(query, insensitive, precise, sources, linesBefore, linesAfter),
                lang = lang
              )
            )
        } unsafeToFuture
      }
    }

  private def callUri(
      query: String,
      insensitive: String,
      precise: String,
      sources: String,
      linesBefore: String,
      linesAfter: String
  ): String = s"""
        |/$lang/search
        |?query=$query
        |&insensitive=$insensitive
        |&precise=$precise
        |&sources=$sources
        |&linesBefore=$linesBefore
        |&linesAfter=$linesAfter
     """

  def source(relativePath: String, query: String, L: Int): Action[AnyContent] =
    Action.async { implicit request =>
      val realPath = s"data/$relativePath"
      OptionT
        .fromOption[Future](searchEngine.packageName(realPath))
        .flatMap(pack => OptionT.liftF(Helper.readFileAsync(realPath).unsafeToFuture).map(s => (pack, s)))
        .map {
          case (pack, code) =>
            Ok(
              views.html.sourceCode(
                sourceCode = code,
                pack = pack,
                relativePath = relativePath.split('/').drop(3).mkString("/"),
                lang = lang,
                query = query,
                firstMatch = L
              )
            )
        }
        .getOrElse(NotFound.apply("Not found"))
    }
}
