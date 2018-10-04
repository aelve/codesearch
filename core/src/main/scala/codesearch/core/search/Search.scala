package codesearch.core.search
import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import cats.effect.IO
import codesearch.core.config.{Config, SnippetConfig}
import codesearch.core.index.repository.Extensions
import codesearch.core.search.Searcher.{CSearchResult, CodeSnippet, Package}
import codesearch.core.util.Helper
import cats.syntax.traverse._
import cats.instances.option._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.search.Search.snippetConfig
import codesearch.core.util.Helper.readFileAsync

import scala.sys.process.Process

private[search] class Search[A <: SearchRequest: Extensions: СSearchDirectory](args: A) {

  def search(): Unit = {
    csearch(args)
  }

  private def csearch(request: SearchRequest): IO[List[String]] = {
    val extraEnv = ("CSEARCHINDEX", СSearchDirectory[A].indexDirAs[String])
    IO((Process(arguments(request), None, extraEnv) #| Seq("head", "-1001")).!!.split('\n').toList)
  }

  private def arguments(request: SearchRequest): List[String] = {
    val forExtensions = if (request.sourcesOnly) extensionsRegex else ".*"
    val query         = if (request.preciseMatch) Helper.hideSymbols(request.query) else request.query
    val insensitive   = if (request.insensitive) "-i" else new String()
    List("csearch", "-n", insensitive, "-f", forExtensions, query)
  }

  private def extensionsRegex: String = Extensions[A].sourceExtensions.mkString(".*\\.(", "|", ")$")

  /**
    * Map code search output to case class
    *
    * @param out csearch console out
    * @return search result
    */
  protected def mapCSearchOutput(out: String): IO[Option[CSearchResult]] = {
    out.split(':').toList match {
      case fullPath :: lineNumber :: _ => result(fullPath, lineNumber.toInt)
      case _                           => IO.pure(None)
    }
  }

  private def result(fullPath: String, lineNumber: Int): IO[Option[CSearchResult]] = {
    val relativePath = Path(fullPath).relativeTo(pwd).toString
    packageName(relativePath).traverse { p =>
      Helper.extractRows(relativePath, lineNumber, snippetConfig.linesBefore, snippetConfig.linesAfter).map {
        case (firstLine, rows) =>
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

  /**
    * Build package name and path to remote repository
    *
    * @param relativePath path to source code
    * @return package name and url to repository
    */
  private def packageName(relativePath: String): Option[Package] = relativePath.split('/').drop(2).toList match {
    case libName :: version :: _ =>
      val decodedName = URLDecoder.decode(libName, "UTF-8")
      Some(Package(s"$decodedName-$version", buildRepUrl(decodedName, version)))
    case _ => None
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
    Config
      .load()
      .toOption
      .map(_.snippetConfig)
      .getOrElse(SnippetConfig(30, 3, 5))
}
