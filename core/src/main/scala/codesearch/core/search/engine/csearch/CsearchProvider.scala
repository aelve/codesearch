package codesearch.core.search.engine.csearch

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import codesearch.core.config.{CindexConfig, SourcesFilesConfig}
import codesearch.core.regex.RegexConstructor
import codesearch.core.search.SearchRequest
import codesearch.core.search.engine.SearchProvider
import fs2.{Pipe, Stream}
import io.chrisdavenport.log4cats.Logger

import scala.sys.process.Process

case class MatchedRow(path: String, lineNumber: Int)

object CsearchProvider {
  def apply[F[_]: Sync](
      sourcesFilesConfig: SourcesFilesConfig,
      cindexConfig: CindexConfig,
      logger: Logger[F]
  ): SearchProvider[F, SearchRequest, Stream[F, MatchedRow]] = (request: SearchRequest) => {

    val indexDir    = cindexConfig.indexDir
    val environment = ("CSEARCHINDEX", indexDir)
    val pipe        = Seq("head", s"-${request.limit}")
    val process     = Process(arguments(request), None, environment) #| pipe

    def parse: Pipe[F, String, MatchedRow] = { lines =>
      lines.map { row =>
        val Array(path, lineNumber) = row.split(":").take(2) //filePath:lineNumber:matchedString
        MatchedRow(path, lineNumber.toInt)
      }
    }

    def arguments(request: SearchRequest): Seq[String] = {
      val searchInFilesRegexp: String = request.filePath match {
        case Some(filePath) => filePath
        case None =>
          if (request.sourcesOnly) {
            val sourcesExtensionsRegexp =
              sourcesFilesConfig.filesExtensions.sourceExtensions.mkString(".*\\.(", "|", ")$")
            if (request.excludeTests) {
              val excludedTestDirsRegexp = sourcesFilesConfig.testDirsNames.mkString("^(?!.*(", "|", "))")
              excludedTestDirsRegexp + sourcesExtensionsRegexp
            } else sourcesExtensionsRegexp
          } else ".*"
      }

      val queryRegex =
        RegexConstructor(request.query, request.insensitive, request.spaceInsensitive, request.preciseMatch)

      request.filter match {
        case Some(filter) => Seq("csearch", "-n", "-f", searchInFilesRegexp, queryRegex, filter)
        case None         => Seq("csearch", "-n", "-f", searchInFilesRegexp, queryRegex)
      }
    }

    for {
      _                <- logger.debug(s"running CSEARCHINDEX=$indexDir ${arguments(request).mkString(" ")}")
      resultRows       <- Sync[F].delay(process.lineStream.toList)
      parsedResultRows <- Stream.emits(resultRows).through(parse).pure[F]
    } yield parsedResultRows
  }
}
