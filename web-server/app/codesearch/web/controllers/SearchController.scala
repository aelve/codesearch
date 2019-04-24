package codesearch.web.controllers

import cats.data.OptionT
import cats.instances.future._
import codesearch.core.db.DefaultDB
import codesearch.core.index.directory.Directory
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
      filter: Option[String],
      filePath: Option[String],
      caseInsensitive: String,
      spaceInsensitive: String,
      precise: String,
      sources: String,
      withoutTests: String,
      page: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      val host: String = request.host
      val searchRequest =
        SearchRequest.applyRaw(lang,
                               query,
                               filter,
                               filePath,
                               caseInsensitive,
                               spaceInsensitive,
                               precise,
                               sources,
                               withoutTests,
                               page)
      println(withoutTests)
      println(searchRequest.withoutTests)
      db.updated.flatMap { updated =>
        searchEngine.search(searchRequest) map {
          case CSearchPage(results, total) =>
            Ok(
              views.html.searchResults(
                updated = TimeAgo.using(updated.getTime),
                packages = results,
                query = searchRequest.query,
                filter = searchRequest.filter,
                filePath = searchRequest.filePath,
                insensitive = searchRequest.insensitive,
                space = searchRequest.spaceInsensitive,
                precise = searchRequest.preciseMatch,
                sources = searchRequest.sourcesOnly,
                withoutTests = searchRequest.withoutTests,
                page = searchRequest.page,
                totalMatches = total,
                callURI = searchRequest.callURI(host).toString,
                lang = lang
              )
            )
        } unsafeToFuture
      }
    }

  def source(relativePath: String, query: String, L: Int): Action[AnyContent] =
    Action.async { implicit request =>
      val realPath = s"${Directory.sourcesDir}/$relativePath"
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
