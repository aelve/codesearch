package codesearch.core.index

import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import codesearch.core.db.DefaultDB
import codesearch.core.index.LanguageIndex._
import codesearch.core.index.directory.Directory
import codesearch.core.index.repository._
import codesearch.core.index.repository.Download.ops._
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
    * Build package name and path to remote repository
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
    * Map code search output to case class
    * @param out csearch console out
    * @return search result
    */
  protected def mapCSearchOutput(out: String): Option[CSearchResult] = {
    val res = out.split(':').toList match {
      case fullPath :: lineNumber :: _ => createCSearchResult(fullPath, lineNumber.toInt)
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

  protected def archiveDownloadAndExtract[A <: SourcePackage: Extensions: Directory](pack: A): Future[Int] = {
    import codesearch.core.index.repository.SourceRepository._
    (for {
      _         <- pack.downloadSources
      rowsCount <- insertOrUpdate(pack)
    } yield rowsCount).recover { case _ => 0 }
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

  private[index] val PAGE_SIZE = 100

  /**
    *
    * @param pack name and link to package
    * @param result matched code snippet
    */
  private[index] final case class CSearchResult(
      pack: Package,
      result: CodeSnippet
  )
}
