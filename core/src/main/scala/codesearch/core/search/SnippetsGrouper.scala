package codesearch.core.search

import cats.data.NonEmptyVector
import cats.instances.string._
import codesearch.core.config.SnippetConfig
import fs2._

object SnippetsGrouper {

  /**
    *  Info about code snippet
    *
    * @param filePath absolute path to file
    * @param lines numbers of matched lines in file
    */
  case class SnippetInfo(filePath: String, lines: NonEmptyVector[Int], totalMatches: Int)

  private case class ResultRow(path: String, lineNumber: Int)

  /**
    * Transforms raw search output in format `filePath:lineNumber:matchedString` to snippets grouping matched lines
    * from the same file if they are close to each other
    *
    * @param config config for creating a snippet
    */
  def groupLines[F[_]](config: SnippetConfig): Pipe[F, String, SnippetInfo] = { lines =>
    for {
      (_, resultRows) <- lines.map { row =>
        val Array(path, lineNumber) = row.split(":").take(2) //filePath:lineNumber:matchedString
        ResultRow(path, lineNumber.toInt)
      }.groupAdjacentBy(_.path)
      snippet <- Stream.emits {
        groupRowsToSnippets(config)(resultRows)
      }
    } yield snippet
  }

  private def groupRowsToSnippets(config: SnippetConfig)(rows: Chunk[ResultRow]): Seq[SnippetInfo] = {
    rows.foldLeft(Vector.empty[SnippetInfo]) { (snippets, row) =>
      snippets.lastOption match {
        case Some(snippet) =>
          if (row.lineNumber < snippet.lines.last + config.linesAfter) {
            snippets.init :+ snippet.copy(lines = snippet.lines :+ row.lineNumber,
                                          totalMatches = snippet.totalMatches + 1)
          } else {
            snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber), 1)
          }
        case None =>
          snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber), 1)
      }
    }
  }
}
