package codesearch.core.util

import java.io.File

import cats.effect.{Resource, Sync}
import fs2.{Chunk, Stream}

import scala.io.Source

object FsUtils {

  def recursiveListFiles[F[_]: Sync](cur: File): Stream[F, File] = {
    val stream        = Stream.evalUnChunk(Sync[F].delay(Chunk.array(cur.listFiles)))
    val files         = stream.filter(_.isFile)
    val filesFromDirs = stream.filter(_.isDirectory).flatMap(recursiveListFiles)
    files ++ filesFromDirs
  }

  def readFileAsync[F[_]: Sync](path: String): F[List[String]] =
    Resource
      .fromAutoCloseable(Sync[F].delay(Source.fromFile(path, "UTF-8")))
      .use(source => Sync[F].delay(source.getLines.toList))
}
