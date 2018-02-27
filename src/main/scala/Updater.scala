import sys.process._
import java.net.URL
import java.io.File

import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import ammonite.ops._
import com.typesafe.scalalogging

object Updater {
  private val INDEX_LINK = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE = pwd / 'data / "index.tar.gz"
  private val SOURCE = pwd / 'data / 'index / "index"

  def update(): Unit = {
    // TODO: Logging
    new URL(INDEX_LINK) #> new File(INDEX_SOURCE.toString()) !!

    val archive = new File(INDEX_SOURCE.toString)
    val destination = new File(SOURCE.toString)

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }
}
