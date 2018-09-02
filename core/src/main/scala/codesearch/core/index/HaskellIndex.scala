package codesearch.core.index

import java.net.URL

import ammonite.ops.{Path, pwd}
import codesearch.core.db.HackageDB
import codesearch.core.index.repository.HackagePackage
import repository.Extensions._
import codesearch.core.index.directory.Directory._

import sys.process._
import codesearch.core.model.{HackageTable, Version}
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class HaskellIndex(val ec: ExecutionContext) extends LanguageIndex[HackageTable] with HackageDB {

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".hackage_csearch_index"
  override protected val langExts: String  = ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$"

  private val INDEX_LINK: String     = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE_GZ: Path  = pwd / 'data / "index.tar.gz"
  private val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  override protected def updateSources(name: String, version: String): Future[Int] = {
    logger.info(s"downloading package $name")
    archiveDownloadAndExtract(HackagePackage(name, version))
  }

  override def downloadMetaInformation(): Unit = {
    logger.info("update index")

    val archive     = INDEX_SOURCE_GZ.toIO
    val destination = INDEX_SOURCE_DIR.toIO

    archive.getParentFile.mkdirs()
    destination.mkdirs()

    new URL(INDEX_LINK) #> archive !!

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }

  override protected def getLastVersions: Map[String, Version] = {
    val indexDir     = INDEX_SOURCE_DIR.toIO
    val packageNames = indexDir.listFiles.filter(_.isDirectory)
    val allVersions = packageNames.flatMap { packagePath =>
      packagePath.listFiles
        .filter(_.isDirectory)
        .map(versionPath => (packagePath.getName, Version(versionPath.getName)))
    }
    val lastVersions = allVersions.groupBy { case (name, _) => name }
      .mapValues(_.map { case (_, version) => version }.max)

    lastVersions
  }

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://hackage.haskell.org/package/$packageName-$version"

  override protected implicit def executor: ExecutionContext = ec
}

object HaskellIndex {
  def apply()(implicit ec: ExecutionContext) = new HaskellIndex(ec)
}
