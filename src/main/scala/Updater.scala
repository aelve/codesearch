import sys.process._
import java.net.URL
import java.io.File

import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import ammonite.ops._

object Updater {
  private val INDEX_LINK = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE = pwd / 'data / "index.tar.gz"
  private val SOURCE = pwd / 'data / 'index / "index"
  private val VERSIONS_FILE = pwd / 'data / "package_versions.json"

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  def update(downloadIndex: Boolean): Unit = {


    if (downloadIndex) {
      new URL(INDEX_LINK) #> new File(INDEX_SOURCE.toString()) !!

      val archive = new File(INDEX_SOURCE.toString)
      val destination = new File(SOURCE.toString)

      val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
      archiver.extract(archive, destination)
    }

    val indexDir = new File(INDEX_SOURCE.toString)
    recursiveListFiles(indexDir).foreach(print)
  }
}
