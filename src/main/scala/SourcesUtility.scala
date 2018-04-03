import java.io.FileOutputStream
import java.net.URL

import ammonite.ops.pwd

import sys.process._
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import com.typesafe.scalalogging.{LazyLogging, Logger}

class SourcesUtility {
}

object SourcesUtility extends LazyLogging {
  def update(downloadIndex: Boolean): Unit = {

    if (downloadIndex) {
      VersionsUtility.updateIndex()
    }

    val currentVersions = VersionsUtility.loadCurrentVersions()
    val lastVersions = VersionsUtility.updateVersions()

    logger.info(s"old size of the index: ${currentVersions.size}")
    logger.info(s"new size of the index: ${lastVersions.size}")

    lastVersions.filterNot { case (name, ver) =>
      currentVersions.get(name).contains(ver)
    }.foreach { case (name, ver) =>
      downloadSources(name, ver)
    }
  }

  def downloadSources(name: String, ver: Version): Unit = {
    logger.info(s"downloading package $name")
    val packageURL =
      s"https://hackage.haskell.org/package/$name-${ver.verString}/$name-${ver.verString}.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver.verString / s"${ver.verString}.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver.verString / ver.verString

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    destination.mkdirs()

    val archiveOS = new FileOutputStream(archive)

    new URL(packageURL) #> archiveOS !!

    archiveOS.flush()

    logger.info(s"downloaded")


    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)

    logger.info(s"downloaded and unarchived $name-$ver package")
  }
}
