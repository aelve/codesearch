package codesearch.core.utilities

import sys.process._

case class Result(link: String, nLine: String)
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
      print(s"bad uri: $uri")
      None
    } else {
      val pathSeq: Seq[String] = elems.head.split('/').drop(8)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          print(s"bad uri: $uri")
          None
        case Some(verName) =>
          val remPath = pathSeq.drop(1).mkString("/")

          Some((verName, Result(
              s"https://hackage.haskell.org/package/$verName/src/$remPath",
              nLine
          )))
      }
    }
  }

}
