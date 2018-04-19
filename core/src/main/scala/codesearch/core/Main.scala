package codesearch.core

import java.io.ByteArrayOutputStream

import sys.process._

import ammonite.ops.{FilePath, pwd}

object Main {

  case class Config(updatePackages: Boolean = false,
                    downloadIndex: Boolean = false,
                    sourcesDir: FilePath = pwd / 'sources
                   )

  private val parser = new scopt.OptionParser[Config]("main") {
    head("\nCodesearch command line interface\n\n")

    opt[Unit]('u', "update-packages") action { (_, c) =>
      c.copy(updatePackages = true)
    } text "update package-index"

    opt[Unit]('d', "download-index") action { (_, c) =>
      c.copy(downloadIndex = true)
    } text "update package-index"
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) foreach { c =>
      if (c.updatePackages) {
        utilities.SourcesUtility.update(c.downloadIndex)
      }
    }
  }

  def csearch(query: String): Seq[(String, String)] = {
    val answer = (Seq("csearch", "-n", query) #| Seq("head", "-1000")) .!!
    answer.split('\n').map(toHackageLink).filterNot(_._1.isEmpty)
  }

  def toHackageLink(uri: String): (String, String) = {

    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      print(s"bad uri: $uri")
      ("", "")
    } else {
      val pathSeq: Seq[String] = elems.head.split('/').drop(8)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          print(s"bad uri: $uri")
          ("", "")
        case Some(verName) =>
          val remPath = pathSeq.drop(1).mkString("/")
          (s"https://hackage.haskell.org/package/$verName/src/$remPath", nLine)
      }
    }
  }
}
