package codesearch.core.util

import java.io.File

import cats.effect.{IO, Resource}
import fs2.{Chunk, Stream}
import org.apache.commons.io.FilenameUtils

import scala.io.Source

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

  def recursiveListFiles(cur: File): Stream[IO, File] = {
    val these = Stream.evalUnChunk(IO(Chunk.array(cur.listFiles)))
    these.filter(_.isFile) ++ these.filter(_.isDirectory).filter(_.getName != ".git").flatMap(recursiveListFiles)
  }

  def readFileAsync(path: String): IO[List[String]] =
    Resource.fromAutoCloseable(IO(Source.fromFile(path, "UTF-8"))).use(source => IO.delay(source.getLines.toList))

  def langByLink(fileLink: String, defaultLang: String): String = {
    val ext = FilenameUtils.getExtension(fileLink)
    langByExt.getOrElse(ext, defaultLang)
  }
}
