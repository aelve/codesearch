import java.io.File

import ammonite.ops.pwd

import sys.process._
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import com.typesafe.scalalogging.{LazyLogging}

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

    downloadFile(packageURL, archive)

    logger.info(s"downloaded")


    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    try {
      archiver.extract(archive, destination)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
    }

    logger.info(s"downloaded and unarchived $name-$ver package")
  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
    val s = s"curl -o ${dstFile.getPath} $srcURL" !!
  }
}
