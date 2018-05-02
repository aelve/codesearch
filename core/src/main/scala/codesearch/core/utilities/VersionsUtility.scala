package codesearch.core.utilities

import sys.process._
import java.net.URL

import ammonite.ops.{Path, pwd}
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import codesearch.core.model.Version
import org.slf4j.{Logger, LoggerFactory}

object VersionsUtility {
  private val logger: Logger = LoggerFactory.getLogger(VersionsUtility.getClass)

  val INDEX_LINK: String = "http://hackage.haskell.org/packages/index.tar.gz"
  val INDEX_SOURCE_GZ: Path = pwd / 'data / "index.tar.gz"
  val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  val VERSIONS_FILE: Path = pwd / 'data / "versions.obj"

  def updateIndex(): Unit = {
    logger.info("update index")

    val archive = INDEX_SOURCE_GZ.toIO
    val destination = INDEX_SOURCE_DIR.toIO

    archive.getParentFile.mkdirs()
    destination.mkdirs()

    new URL(INDEX_LINK) #> archive !!

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }

  def getLastVersions: Map[String, Version] = {
    logger.info("update Versions")

    val indexDir = VersionsUtility.INDEX_SOURCE_DIR.toIO
    val packageNames = indexDir.listFiles.filter(_.isDirectory)

    val lastVersions = packageNames.flatMap(packagePath =>
      packagePath.listFiles.filter(_.isDirectory).map(versionPath =>
        (packagePath.getName, Version(versionPath.getName))
      )
    ).groupBy(_._1).mapValues(_.map(_._2).max)

    lastVersions
  }
}
