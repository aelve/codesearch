package codesearch.core.util

import java.io.File

import cats.effect.IO
import org.apache.commons.io.FilenameUtils

import scala.io.Source
import scala.util.matching.Regex
import codesearch.core.regex.lexer.tokens._
import codesearch.core.regex.lexer._
import codesearch.core.regex.space.SpaceInsensitive

object Helper {

  val langByExt: Map[String, String] = Map(
    "hs"   -> "haskell",
    "md"   -> "markdown",
    "cpp"  -> "cpp",
    "c"    -> "c",
    "h"    -> "c",
    "hpp"  -> "cpp",
    "js"   -> "javascript",
    "css"  -> "css",
    "java" -> "java",
    "rs"   -> "rust",
    "yml"  -> "yaml",
    "yaml" -> "yaml",
    "json" -> "json",
    "rb"   -> "ruby"
  )

  def recursiveListFiles(cur: File): Array[File] = {
    val these = cur.listFiles
    these.filter(_.isFile) ++ these.filter(_.isDirectory).filter(_.getName != ".git").flatMap(recursiveListFiles)
  }

  def readFileAsync(path: String): IO[List[String]] =
    IO(Source.fromFile(path, "UTF-8")).bracket(source => IO.pure(source.getLines.toList))(source => IO(source.close()))

  def preciseMatch(query: String): String = {
    val queryTokens: Seq[Token] = Tokenizer.parseStringWithSpecialSymbols(query)
    val preciseMatch: Seq[Token] = queryTokens.map {
      case literal @ Literal(_) => literal
      case token: Token         => Escaped(token.repr)
      case other                => other
    }
    StringAssembler.buildStringFromTokens(preciseMatch)
  }

  def langByLink(fileLink: String, defaultLang: String): String = {
    val ext = FilenameUtils.getExtension(fileLink)
    langByExt.getOrElse(ext, defaultLang)
  }

  def buildRegex(query: String, insensitive: Boolean, space: Boolean, precise: Boolean): Regex = {
    val preciseAndSpace: String = {
      val preciseMatch = if (precise) Helper.preciseMatch(query) else query

      if (space) SpaceInsensitive.spaceInsensitiveString(preciseMatch) else preciseMatch
    }

    val regex: String = {
      val insensitiveCase = if (insensitive) "(?i)" else ""
      insensitiveCase + preciseAndSpace
    }
    println(regex)
    regex.r
  }

}
