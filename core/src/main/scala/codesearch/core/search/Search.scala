package codesearch.core.search

import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import cats.data.NonEmptyVector
import cats.effect.IO
import cats.syntax.option._
import codesearch.core.config.{Config, SnippetConfig}
import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.search.Search.{CSearchPage, CSearchResult, CodeSnippet, Package, PackageResult, snippetConfig}
import codesearch.core.search.SnippetsGrouper.SnippetInfo
import codesearch.core.util.Helper.readFileAsync
import fs2.{Pipe, Pure, Stream}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import codesearch.core.regex.RegexConstructor
import codesearch.core.syntax.stream._

import scala.sys.process.Process

trait Search {

  protected def cindexDir: СindexDirectory
  protected def extensions: Extensions
  protected val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.unsafeCreate[IO]
  def isTestInPath(path: String): Boolean

  def search(request: SearchRequest): IO[CSearchPage] = {
    for {
      lines <- csearch(request)

      snippetsInfo = Stream
        .emits(lines)
        .through(SnippetsGrouper.groupLines(snippetConfig))

      filteredSnippetsInfo = if (request.withoutTests)
        snippetsInfo.filterNot(snippetInfo => isTestInPath(snippetInfo.filePath))
      else
        snippetsInfo

      results <- filteredSnippetsInfo
        .drop(snippetConfig.pageSize * (request.page - 1))
        .take(snippetConfig.pageSize)
        .evalMap(createSnippet)
        .through(groupByPackage)
        .compile
        .toList
      totalMatches = filteredSnippetsInfo.fold(0)((total, snippet) => total + snippet.totalMatches).compile.toList.last
    } yield CSearchPage(results.sortBy(_.pack.name), totalMatches)
  }

  /**
    * Build package name and path to remote repository
    *
    * @param relativePath path to source code
    * @return package name and url to repository
    */
  def packageName(relativePath: String): Option[Package] = {
    val slashes = if (relativePath.startsWith("./")) 4 else 3
    relativePath.split('/').drop(slashes).toList match {
      case libName :: version :: _ =>
        val decodedName = URLDecoder.decode(libName, "UTF-8")
        Some(Package(s"$decodedName-$version", buildRepUrl(decodedName, version)))
      case _ => None
    }
  }

  /**
    * Create link to remote repository.
    *
    * @param packageName local package name
    * @param version of package
    * @return link
    */
  protected def buildRepUrl(packageName: String, version: String): String

  private def csearch(request: SearchRequest): IO[List[String]] = {
    val indexDir = cindexDir.indexDirAs[String]
    val env      = ("CSEARCHINDEX", indexDir)

    for {
      _       <- logger.debug(s"running CSEARCHINDEX=$indexDir ${arguments(request).mkString(" ")}")
      results <- IO((Process(arguments(request), None, env) #| Seq("head", "-1001")).lineStream.toList)
    } yield results
  }

  private def arguments(request: SearchRequest): List[String] = {
    def extensionsRegex: String = extensions.sourceExtensions.mkString(".*\\.(", "|", ")$")

    val forExtensions: String = request.filePath match {
      case Some(filePath) => filePath
      case None           => if (request.sourcesOnly) extensionsRegex else ".*"
    }

    val query: String =
      RegexConstructor(request.query, request.insensitive, request.spaceInsensitive, request.preciseMatch)

    request.filter match {
      case Some(filter) => List("csearch", "-n", "-f", forExtensions, query, filter)
      case None         => List("csearch", "-n", "-f", forExtensions, query)
    }
  }

  private def groupByPackage: Pipe[IO, CSearchResult, PackageResult] = { csearchResults =>
    csearchResults.groupAdjacentBy(_.pack)(_ == _).map {
      case (pack, results) => PackageResult(pack, results.map(_.result).toList)
    }
  }

  private def createSnippet(info: SnippetInfo): IO[CSearchResult] = {
    val relativePath = Path(info.filePath).relativeTo(pwd).toString
    packageName(relativePath).liftTo[IO](new RuntimeException(s"Not found $relativePath")).flatMap { `package` =>
      extractRows(relativePath, info.lines, snippetConfig.linesBefore, snippetConfig.linesAfter).map {
        case (firstLine, rows) =>
          CSearchResult(
            `package`,
            CodeSnippet(
              relativePath.split('/').drop(5).mkString("/"), // drop `data/packages/hackage/packageName/version/`
              relativePath.split('/').drop(2).mkString("/"), // drop `data/packages/`
              firstLine,
              info.lines,
              rows
            )
          )
      }
    }
  }

  /**
    * Returns index of first matched lines and lines of code
    */
  private def extractRows(
      path: String,
      codeLines: NonEmptyVector[Int],
      beforeLines: Int,
      afterLines: Int
  ): IO[(Int, Seq[String])] = {
    readFileAsync(path).map { lines =>
      val (code, indexes) = lines.zipWithIndex.filter {
        case (_, index) => index >= codeLines.head - beforeLines - 1 && index <= codeLines.last + afterLines - 1
      }.unzip
      indexes.head -> code
    }
  }

}

object Search {

  val snippetConfig: SnippetConfig =
    Config
      .load[IO]
      .map(_.snippetConfig)
      .unsafeRunSync()

  /**
    * result of searching
    *
    * @param data code snippets grouped by package
    * @param total number of total matches
    */
  final case class CSearchPage(
      data: Seq[PackageResult],
      total: Int
  )

  /**
    *
    * @param relativePath path into package sources
    * @param fileLink link to file with source code (relative)
    * @param numberOfFirstLine number of first line in snippet from source file
    * @param matchedLines numbers of matched lines in snippet from source file
    * @param lines lines of snippet
    */
  final case class CodeSnippet(
      relativePath: String,
      fileLink: String,
      numberOfFirstLine: Int,
      matchedLines: NonEmptyVector[Int],
      lines: Seq[String]
  )

  /**
    * Grouped code snippets by package
    *
    * @param pack name and link to package
    * @param results code snippets
    */
  final case class PackageResult(
      pack: Package,
      results: Seq[CodeSnippet]
  )

  /**
    * Representation of package
    *
    * @param name of package
    * @param packageLink to remote repository
    */
  final case class Package(name: String, packageLink: String)

  /**
    *
    * @param pack name and link to package
    * @param result matched code snippet
    */
  private[search] final case class CSearchResult(
      pack: Package,
      result: CodeSnippet
  )
}
