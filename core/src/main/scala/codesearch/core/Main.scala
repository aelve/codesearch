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

  def csearch(query: String): String = {
    val baos = new ByteArrayOutputStream()
    s"csearch '$query'" #> baos !!;
    baos.toString
  }
}
