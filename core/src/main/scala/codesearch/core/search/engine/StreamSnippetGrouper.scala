package codesearch.core.search.engine

import cats.Applicative
import cats.data.NonEmptyVector
import codesearch.core.config.SnippetConfig
import codesearch.core.search.engine.csearch.MatchedRow
import fs2._
import cats.instances.string._

/**
  *  Info about code snippet
  *
  * @param filePath absolute path to file
  * @param lines numbers of matched lines in file
  */
case class SnippetInfo(filePath: String, lines: NonEmptyVector[Int])

final class StreamSnippetGrouper[F[_]: Applicative](config: SnippetConfig) {

  def group: Pipe[F, MatchedRow, SnippetInfo] = { matchedRows =>
    for {
      (_, matchedRow) <- matchedRows.groupAdjacentBy(_.path)
      snippets        <- Stream.emits(groupToSnippets(matchedRow))
    } yield snippets

    def groupToSnippets(rows: Chunk[MatchedRow]): Seq[SnippetInfo] = {
      rows.foldLeft(Vector.empty[SnippetInfo]) { (snippets, row) =>
        snippets.lastOption match {
          case Some(snippet) =>
            if (row.lineNumber < snippet.lines.last + config.linesAfter)
              snippets.init :+ snippet.copy(lines = snippet.lines :+ row.lineNumber)
            else
              snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber))
          case None =>
            snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber))
        }
      }
    }
  }

}

object StreamSnippetGrouper {
  def apply[F[_]: Applicative](
      config: SnippetConfig
  ): StreamSnippetGrouper[F] = new StreamSnippetGrouper[F](config)
}
