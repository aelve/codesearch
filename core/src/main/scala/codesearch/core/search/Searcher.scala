package codesearch.core.search
import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import codesearch.core.search.Searcher._
import codesearch.core.util.Helper
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process

trait Searcher {

  protected val logger: Logger

  protected def langExts: String
  protected def indexFile: String
  protected def indexPath: Path = pwd / 'data / indexFile

  def search(args: SearchArguments, page: Int)(implicit ec: ExecutionContext): Future[CSearchPage] = {
    runCsearch(args).map { answers =>
      val data = answers
        .slice(math.max(page - 1, 0) * PageSize, page * PageSize)
        .flatMap(mapCSearchOutput)
        .groupBy(x => x.pack)
        .map {
          case (pack, results) =>
            PackageResult(pack, results.map(_.result).toSeq)
        }
        .toSeq
        .sortBy(_.pack.name)
      CSearchPage(data, answers.length)
    }
  }

  /**
    * Map code search output to case class
    *
    * @param out csearch console out
    * @return search result
    */
  protected def mapCSearchOutput(out: String): Option[CSearchResult] = {
    val res = out.split(':').toList match {
      case fullPath :: lineNumber :: _ => createCSearchResult(fullPath, lineNumber.toInt)
      case _                           => None
    }
    if (res.isEmpty) logger.warn(s"bad codesearch output: $out")
    res
  }

  /**
    * Build package name and path to remote repository
    *
    * @param relativePath path to source code
    * @return package name and url to repository
    */
  def packageName(relativePath: String): Option[Package] = relativePath.split('/').drop(2).toList match {
    case libName :: version :: _ =>
      val decodedName = URLDecoder.decode(libName, "UTF-8")
      Some(Package(s"$decodedName-$version", buildRepUrl(decodedName, version)))
    case _ => None
  }

  /**
    * Create link to remote repository.
    *
    * @param packageName local package name
    * @param version of package
    * @return link
    */
  protected def buildRepUrl(packageName: String, version: String): String

  private def createCSearchResult(fullPath: String, lineNumber: Int): Option[CSearchResult] = {
    val relativePath = Path(fullPath).relativeTo(pwd).toString
    packageName(relativePath).map { p =>
      val (firstLine, rows) = Helper.extractRows(relativePath, lineNumber)
      CSearchResult(
        p,
        CodeSnippet(
          relativePath.split('/').drop(4).mkString("/"), // drop `data/hackage/packageName/version/`
          relativePath.split('/').drop(1).mkString("/"), // drop `data`
          firstLine,
          lineNumber - 1,
          rows
        )
      )
    }
  }

  protected def runCsearch(arg: SearchArguments)(implicit ec: ExecutionContext): Future[Array[String]] = {
    val pathRegex     = if (arg.sourcesOnly) langExts else ".*"
    val query: String = if (arg.preciseMatch) Helper.hideSymbols(arg.query) else arg.query

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (arg.insensitive) {
      args.append("-i")
    }
    args.append("-f", pathRegex)
    args.append(query)

    Future {
      (Process(args, None, "CSEARCHINDEX" -> indexPath.toString()) #| Seq("head", "-1001")).!!.split('\n')
    }
  }

}

object Searcher {

  private[search] val PageSize = 100

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
    * @param query input regular expression
    * @param insensitive insensitive flag
    * @param preciseMatch precise match flag
    * @param sourcesOnly sources only flag
    */
  final case class SearchArguments(
      query: String,
      insensitive: Boolean,
      preciseMatch: Boolean,
      sourcesOnly: Boolean
  )

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
