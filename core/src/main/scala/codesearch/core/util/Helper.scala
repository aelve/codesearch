package codesearch.core.util

import java.io.File

import cats.effect.IO
import org.apache.commons.io.FilenameUtils

import scala.collection.mutable
import scala.io.Source
import scala.util.matching.Regex

object Helper {

  private val SPECIAL_CHARS = "$^*+().?|"

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

  /**
    * Returns index of first matched lines and lines of code
    */
  def extractRows(path: String, codeLine: Int, beforeLines: Int, afterLines: Int): IO[(Int, Seq[String])] = {
    readFileAsync(path).map { lines =>
      val (code, indexes) = lines.zipWithIndex.filter {
        case (_, index) => index >= codeLine - beforeLines - 1 && index <= codeLine + afterLines
      }.unzip

      indexes.head -> code
    }
  }

  def readFileAsync(path: String): IO[List[String]] =
    IO(Source.fromFile(path, "UTF-8")).bracket(source => IO.pure(source.getLines.toList))(source => IO(source.close()))

  def hideSymbols(str: String): String = {
    str.foldRight("") {
      case (c, res) if SPECIAL_CHARS contains c =>
        s"\\$c$res"
      case (c, res) =>
        s"$c$res"
    }
  }

  def langByLink(fileLink: String, defaultLang: String): String = {
    val ext = FilenameUtils.getExtension(fileLink)
    langByExt.getOrElse(ext, defaultLang)
  }

  def buildRegex(query: String, insensitive: Boolean, precise: Boolean): Regex = {
    val regex = mutable.StringBuilder.newBuilder
    if (insensitive) {
      regex.append("(?i)")
    }
    regex.append {
      if (precise) {
        Helper.hideSymbols(query)
      } else {
        query
      }
    }
    regex.toString.r
  }

}
