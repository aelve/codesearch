package codesearch.core.index

import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import codesearch.core.db.DefaultDB
import codesearch.core.index.LanguageIndex._
import codesearch.core.index.directory.Directory
import codesearch.core.index.repository.{Download, Extension, SourcePackage}
import codesearch.core.model.{DefaultTable, Version}
import codesearch.core.util.Helper
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

trait LanguageIndex[VTable <: DefaultTable] { self: DefaultDB[VTable] =>
  protected val logger: Logger
  protected val langExts: String

  protected val indexFile: String
  protected lazy val indexPath: Path = pwd / 'data / indexFile

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def downloadMetaInformation(): Unit

  /**
    * download all latest packages version
    *
    * @return count of updated packages
    */
  def updatePackages(): Future[Int] = {
    Future
      .successful(logger.debug("UPDATE PACKAGES"))
      .map(_ => getLastVersions)
      .map(_.mapValues(_.verString))
      .flatMap { versions =>
        verNames().flatMap { packages =>
          val packagesMap = Map(packages: _*)
          Future.sequence(versions.filter {
            case (packageName, currentVersion) =>
              !packagesMap.get(packageName).contains(currentVersion)
          }.map {
            case (packageName, currentVersion) =>
              updateSources(packageName, currentVersion)
          })
        }
      }
      .map(_.sum)
  }

  def search(args: SearchArguments, page: Int): Future[CSearchPage] = {
    runCsearch(args).map { answers =>
      val data = answers
        .slice(math.max(page - 1, 0) * LanguageIndex.PAGE_SIZE, page * LanguageIndex.PAGE_SIZE)
        .flatMap(mapCSearchOutput)
        .groupBy(x => (x.name, x.url))
        .map {
          case ((verName, packageLink), results) =>
            PackageResult(verName, packageLink, results.map(_.result).toSeq)
        }
        .toSeq
        .sortBy(_.name)
      CSearchPage(data, answers.length)
    }
  }

  /**
    * Map code search output to case class
    * @param out csearch console out
    * @return search result
    */
  protected def mapCSearchOutput(out: String): Option[CSearchResult] = {
    val res = out.split(':').toList match {
      case fullPath :: lineNumber :: _ => createCSearhResult(fullPath, lineNumber.toInt)
      case _                           => None
    }
    if (res.isEmpty) {
      logger.warn(s"bad codesearch output: $out")
    }
    res
  }

  /**
    * Create link to remote repository.
    * @param packageName local package name
    * @param version of package
    * @return link
    */
  protected def buildRepUrl(packageName: String, version: String): String = ""

  protected implicit def executor: ExecutionContext

  protected def archiveDownloadAndExtract[A <: SourcePackage: Extension: Directory](pack: A): Future[Int] = {
    import codesearch.core.index.repository.SourceRepository._
    Download[A]
      .downloadSources(pack)
      .flatMap(_ => insertOrUpdate(pack))
      .recover { case _ => 0 }
  }

  protected def runCsearch(arg: SearchArguments): Future[Array[String]] = {
    val pathRegex     = if (arg.sourcesOnly) langExts else ".*"
    val query: String = if (arg.preciseMatch) Helper.hideSymbols(arg.query) else arg.query

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (arg.insensitive) {
      args.append("-i")
    }
    args.append("-f", pathRegex)
    args.append(query)
    logger.debug(indexPath.toString())

    Future {
      (Process(args, None, "CSEARCHINDEX" -> indexPath.toString()) #| Seq("head", "-1001")).!!.split('\n')
    }
  }

  /**
    * Collect last versions of packages in local folder
    * Key for map is package name, value is last version
    *
    * @return last versions of packages
    */
  protected def getLastVersions: Map[String, Version]

  /**
    * Update source code from remote repository
    *
    * @see [[https://github.com/aelve/codesearch/wiki/Codesearch-developer-Wiki#updating-packages]]
    * @param name of package
    * @param version of package
    * @return count of downloaded files (source files)
    */
  protected def updateSources(name: String, version: String): Future[Int]

  private def createCSearhResult(fullPath: String, lineNumber: Int): Option[CSearchResult] = {
    val relativePath = Path(fullPath).relativeTo(pwd).toString
    relativePath.split('/').drop(2).toList match {
      case libName :: version :: path =>
        val decodedName       = URLDecoder.decode(libName, "UTF-8")
        val (firstLine, rows) = Helper.extractRows(relativePath, lineNumber)
        Some(
          CSearchResult(s"$decodedName-$version",
                        buildRepUrl(decodedName, version),
                        CodeSnippet(
                          path.mkString("/"),
                          firstLine,
                          lineNumber - 1,
                          rows
                        )))
      case _ =>
        None
    }
  }
}

object LanguageIndex {

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
    * @param fileLink link to file with source code (relative)
    * @param numberOfFirstLine number of first line in snippet from source file
    * @param matchedLine number of matched line in snippet from source file
    * @param ctxt lines of snippet
    */
  final case class CodeSnippet(
      fileLink: String,
      numberOfFirstLine: Int,
      matchedLine: Int,
      ctxt: Seq[String]
  )

  /**
    * Grouped code snippets by package
    *
    * @param name name of package
    * @param packageLink link to package source
    * @param results code snippets
    */
  final case class PackageResult(
      name: String,
      packageLink: String,
      results: Seq[CodeSnippet]
  )

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

  private[index] val PAGE_SIZE = 100

  /**
    *
    * @param name name of package
    * @param url link to package source
    * @param result matched code snippet
    */
  private[index] final case class CSearchResult(
      name: String,
      url: String,
      result: CodeSnippet
  )
}
