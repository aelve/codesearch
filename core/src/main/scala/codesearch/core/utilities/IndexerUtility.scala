package codesearch.core.utilities

import scala.collection.mutable
import scala.io.Source
import sys.process._

case class Result(link: String, firstLine: Int, nLine: Int, ctxt: Seq[String])
case class PackageResult(verName: String, results: Seq[Result])

object IndexerUtility {

  def csearch(query: String): Seq[PackageResult] = {
    val answer = (Seq("csearch", "-n", query) #| Seq("head", "-1000")) .!!

    answer.split('\n').flatMap(toHackageLink).groupBy(_._1).map {
      case (verName, results) =>
        PackageResult(verName, results.map(_._2).toSeq)
    }.toSeq
  }

  def toHackageLink(uri: String): Option[(String, Result)] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      println(s"bad uri: $uri")
      None
    } else {
      val fullPath = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(8)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(verName) =>
          val (firstLine, rows) = extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some((verName, Result(
              s"https://hackage.haskell.org/package/$verName/src/$remPath",
              firstLine,
              nLine.toInt - 1,
              rows
          )))
      }
    }
  }

  def extractRows(path: String, i: Int): (Int, Seq[String]) = {
    val lines = Source.fromFile(path).getLines
    val result = mutable.Buffer[String]()
    var firstLine = -1
    lines.zipWithIndex.takeWhile(_._2 <= i+1).foreach {
      case (line, ind) =>
        if (ind >= i - 2) {
          if (firstLine < 0) firstLine = ind
          result.append(line)
        }
    }
    (firstLine, result)
  }

}
