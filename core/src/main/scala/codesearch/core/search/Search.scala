package codesearch.core.search

import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import cats.effect.IO
import cats.instances.list._
import cats.instances.option._
import cats.syntax.traverse._
import codesearch.core.config.{Config, SnippetConfig}
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions
import codesearch.core.search.Search.{CSearchPage, CSearchResult, CodeSnippet, Package, PackageResult, snippetConfig}
import codesearch.core.util.Helper
import codesearch.core.util.Helper.readFileAsync
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import codesearch.core.regex.space.SpaceInsensitive

import scala.sys.process.Process

trait Search {

  protected type Tag
  protected def csearchDir: СSearchDirectory[Tag]
  protected def extensions: Extensions[Tag]
  protected val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.unsafeCreate[IO]

  def search(request: SearchRequest): IO[CSearchPage] = {
    for {
      results        <- csearch(request)
      toOutput       <- resultsFound(results, request.page)
      groupedResults <- groupResults(toOutput)
    } yield CSearchPage(groupedResults, results.size)
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
    val indexDir = csearchDir.indexDirAs[String]
    val env      = ("CSEARCHINDEX", indexDir)
    for {
      _       <- logger.debug(s"running CSEARCHINDEX=$indexDir ${arguments(request).mkString(" ")}")
      results <- IO((Process(arguments(request), None, env) #| Seq("head", "-1001")).!!.split('\n').toList)
    } yield results
  }

  private def arguments(request: SearchRequest): List[String] = {
    def extensionsRegex: String = extensions.sourceExtensions.mkString(".*\\.(", "|", ")$")
    val forExtensions: String   = if (request.sourcesOnly) extensionsRegex else ".*"
    val query: String = {
      val preciseMatch: String = if (request.preciseMatch) Helper.preciseMatch(request.query) else request.query
      if (request.spaceInsensitive) SpaceInsensitive.spaceInsensitiveString(preciseMatch) else preciseMatch
    }
    if (request.insensitive) {
      List("csearch", "-n", "-i", "-f", forExtensions, query, request.filter)
    } else {
      List("csearch", "-n", "-f", forExtensions, query, request.filter)
    }
  }

  private def resultsFound(found: List[String], page: Int): IO[List[Option[CSearchResult]]] = {
    val from  = math.max(page - 1, 0) * snippetConfig.pageSize
    val until = page * snippetConfig.pageSize
    found
      .slice(from, until)
      .traverse(mapCSearchOutput)
  }

  private def groupResults(csearchResults: List[Option[CSearchResult]]): IO[Seq[PackageResult]] = IO(
    csearchResults.flatten
      .groupBy(_.pack)
      .map { case (pack, results) => PackageResult(pack, results.map(_.result)) }
      .toSeq
      .sortBy(_.pack.name)
  )

  /**
    * Map code search output to case class
    *
    * @param out csearch console out
    * @return search result
    */
  private def mapCSearchOutput(out: String): IO[Option[CSearchResult]] = {
    out.split(':').toList match {
      case fullPath :: lineNumber :: _ => csearchResult(fullPath, lineNumber.toInt)
      case _                           => IO.pure(None)
    }
  }

  private def csearchResult(fullPath: String, lineNumber: Int): IO[Option[CSearchResult]] = {
    val relativePath = Path(fullPath).relativeTo(pwd).toString
    packageName(relativePath).traverse { p =>
      extractRows(relativePath, lineNumber, snippetConfig.linesBefore, snippetConfig.linesAfter).map {
        case (firstLine, rows) =>
          CSearchResult(
            p,
            CodeSnippet(
              relativePath.split('/').drop(5).mkString("/"), // drop `data/packages/hackage/packageName/version/`
              relativePath.split('/').drop(2).mkString("/"), // drop `data/packages/`
              firstLine,
              lineNumber - 1,
              rows
            )
          )
      }
    }
  }

  /**
    * Returns index of first matched lines and lines of code
    */
  private def extractRows(path: String, codeLine: Int, beforeLines: Int, afterLines: Int): IO[(Int, Seq[String])] = {
    readFileAsync(path).map { lines =>
      val (code, indexes) = lines.zipWithIndex.filter {
        case (_, index) => index >= codeLine - beforeLines - 1 && index <= codeLine + afterLines
      }.unzip
      indexes.head -> code
    }
  }

}

object Search {

  private[search] val snippetConfig: SnippetConfig =
    Config.load
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
    * @param matchedLine number of matched line in snippet from source file
    * @param ctxt lines of snippet
    */
  final case class CodeSnippet(
      relativePath: String,
      fileLink: String,
      numberOfFirstLine: Int,
      matchedLine: Int,
      ctxt: Seq[String]
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
