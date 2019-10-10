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

  private case class ResultRow(path: String, lineNumber: Int, matchedString: Array[String])

  /**
    * Transforms raw search output in format `filePath:lineNumber:matchedString` to snippets grouping matched lines
    * from the same file if they are close to each other
    *
    * @param config config for creating a snippet
    */
  def groupLines[F[_]](config: SnippetConfig, query: String): Pipe[F, String, SnippetInfo] = { lines =>
    for {
      (_, resultRows) <- lines.map { row =>
        val Array(path, lineNumber) = row.split(":").take(2) // filePath:lineNumber:matchedString
        ResultRow(path, lineNumber.toInt, row.split(":").drop(2))
      }.groupAdjacentBy(_.path)
      snippet <- Stream.emits {
        groupRowsToSnippets(config, query)(resultRows)
      }
    } yield snippet
  }

  private def groupRowsToSnippets(config: SnippetConfig, query: String)(rows: Chunk[ResultRow]): Seq[SnippetInfo] = {
    rows.foldLeft(Vector.empty[SnippetInfo]) { (snippets, row) =>
      val count = math.max(countSubstring(row.matchedString, query), 1)
      snippets.lastOption match {
        case Some(snippet) =>
          if (row.lineNumber < snippet.lines.last + config.linesAfter) {
            snippets.init :+ snippet.copy(lines = snippet.lines :+ row.lineNumber,
              totalMatches = snippet.totalMatches + count)
          } else {
            snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber), count)
          }
        case None =>
          snippets :+ SnippetInfo(row.path, NonEmptyVector.one(row.lineNumber), count)
      }
    }
  }

  private def countSubstring(str: Array[String], sub: String): Int = {
    str.foldLeft(0) { (count, str) =>
      count + str.sliding(sub.length).count(_ == sub)
    }
  }
}
