package codesearch.core.util

import java.io.File

import org.apache.commons.io.FilenameUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.io.Source


object Helper {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val SPECIAL_CHARS = "$^*+().?|"

  val langByExt: Map[String, String] = Map(
    "hs" -> "haskell",
    "md" -> "markdown",
    "cpp" -> "cpp",
    "c" -> "c",
    "h" -> "c",
    "hpp" -> "cpp",
    "js" -> "javascript",
    "css" -> "css",
    "java" -> "java",
    "rs" -> "rust",
    "yml" -> "yaml",
    "yaml" -> "yaml",
    "json" -> "json",
    "rb" -> "ruby"
  )


  def recursiveListFiles(cur: File): Array[File] = {
    val these = cur.listFiles
    these.filter(_.isFile) ++ these.filter(_.isDirectory).filter(_.getName != ".git").flatMap(recursiveListFiles)
  }

  def extractRows(path: String, i: Int): (Int, Seq[String]) = {
    try {
      val lines = Source.fromFile(path, "UTF-8").getLines
      val result = mutable.Buffer[String]()
      var firstLine = -1
      lines.zipWithIndex.takeWhile(_._2 <= i + 1).foreach {
        case (line, ind) =>
          if (ind >= i - 2) {
            if (firstLine < 0) firstLine = ind
            result.append(line)
          }
      }
      (firstLine, result)
    } catch {
      case e: Exception =>
        logger.debug(e.getMessage)
        (0, Seq.empty[String])
    }
  }

  def hideSymbols(str: String): String = {
    str.foldRight("") {
      case (c, res) if SPECIAL_CHARS contains c =>
        s"\\$c$res"
      case (c, res) =>
        s"$c$res"
    }
  }

  def linkByLang(lang: String, packageLink: String, fileLink: String): String = lang match {
    case "haskell" =>
      packageLink + "/src/" + fileLink
    case "rust" =>
      packageLink + "/source/" + fileLink
    case _ =>
      ""
  }

  def langByLink(fileLink: String, defaultLang: String): String = {
    val ext = FilenameUtils.getExtension(fileLink)
    langByExt.getOrElse(ext, defaultLang)
  }
}
