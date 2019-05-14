package codesearch.core.search.engine

import cats.effect.Sync
import codesearch.core.search.SearchRequest
import codesearch.core.search.engine.csearch.MatchedRow
import cats.syntax.flatMap._
import codesearch.core.config.SnippetConfig
import fs2.Stream

sealed trait Response
case class ErrorResponse(message: String)  extends Response
case class SuccessfulResponse[T](value: T) extends Response

object CodeSearcher {
  def apply[F[_]: Sync](
      csearchProvider: SearchProvider[F, SearchRequest, Stream[F, MatchedRow]],
      snippetConfig: SnippetConfig
  ): SearchProvider[F, SearchRequest, Response] = (request: SearchRequest) => {
    val snippetGrouper = StreamSnippetGrouper(snippetConfig)
    val matchedRows    = csearchProvider.searchBy(request)
    val a = matchedRows.flatMap { rows =>
      rows.through(snippetGrouper.group).through()
    }
  }
}
