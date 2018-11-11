package codesearch.core.search

import cats.data.NonEmptyList
import cats.instances.int._
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
  case class SnippetInfo(filePath: String, lines: NonEmptyList[Int]) {
    def merge(row: ResultRow): SnippetInfo = copy(lines = row.lineNumber :: lines)
    def sorted: SnippetInfo                = copy(lines = lines.sorted)
  }

  private case class ResultRow(path: String, lineNumber: Int) {
    def createSnippet = SnippetInfo(path, NonEmptyList.one(lineNumber))
  }

  private sealed trait GroupingStep {
    def result: List[SnippetInfo] = this match {
      case Step(_, current, all) => current.sorted :: all.map(_.sorted)
      case Initial               => Nil
    }
  }
  private case class Step(lastLine: Int, current: SnippetInfo, all: List[SnippetInfo] = Nil) extends GroupingStep
  private case object Initial                                                                extends GroupingStep

  /**
    * Transforms raw search output to snippets grouping matched lines
    * from the same file if they are close to each other
    *
    * @param lines raw search output in format filePath:lineNumber:matchedString
    * @param config config for creating a snippet
    */
  def groupLines[F[_]](config: SnippetConfig): Pipe[F, String, SnippetInfo] = { lines =>
    for {
      (_, resultRows) <- lines
        .through(toResultRow)
        .groupAdjacentBy(_.path)
      snippet <- Stream.emits {
        resultRows.foldLeft(Initial: GroupingStep)(nextStep(config)).result
      }
    } yield snippet
  }

  private def toResultRow[F[_]]: Pipe[F, String, ResultRow] = _.map { row =>
    val Array(path, lineNumber) = row.split(":").take(2)
    ResultRow(path, lineNumber.toInt)
  }

  /**
    * Checks that snippets can be merged or saves the current snippet to accumulator otherwise
    */
  private def nextStep(config: SnippetConfig)(step: GroupingStep, nextRow: ResultRow): GroupingStep = step match {
    case Step(lastLine, snippetInfo, snippets) =>
      if (nextRow.lineNumber < lastLine + config.linesAfter) {
        Step(nextRow.lineNumber, snippetInfo.merge(nextRow), snippets)
      } else {
        Step(nextRow.lineNumber, nextRow.createSnippet, snippetInfo :: snippets)
      }
    case Initial =>
      Step(nextRow.lineNumber, nextRow.createSnippet)
  }

}
