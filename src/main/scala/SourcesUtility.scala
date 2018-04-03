import java.io.FileOutputStream
import java.net.URL

import ammonite.ops.pwd

import sys.process._
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import com.typesafe.scalalogging.Logger

class SourcesUtility {
}

object SourcesUtility {
  private val logger = Logger[SourcesUtility]

  def update(downloadIndex: Boolean): Unit = {

    if (downloadIndex) {
      VersionsUtility.updateIndex()
    }

    val currentVersions = VersionsUtility.loadCurrentVersions()
    val lastVersions = VersionsUtility.updateVersions()

    println(currentVersions.size)
    println(lastVersions.size)

    lastVersions.filterNot { case (name, ver) =>
      currentVersions.get(name).contains(ver)
    }.foreach { case (name, ver) =>
      downloadSources(name, ver)
    }
  }

  def downloadSources(name: String, ver: Version): Unit = {
    val packageURL =
      s"https://hackage.haskell.org/package/$name-$ver/$name-$ver.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver.verString / s"${ver.verString}.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver.verString / ver.verString

    new URL(packageURL) #> new FileOutputStream(packageFileGZ.toIO) !!

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)

    logger.info(s"downloaded and unarchived $name-$ver package")
  }
}
