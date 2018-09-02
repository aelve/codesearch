package codesearch.core.index

import java.net.URL

import ammonite.ops.{Path, pwd}
import codesearch.core.db.HackageDB
import codesearch.core.index.LanguageIndex.{CSearchResult, CodeSnippet}
import codesearch.core.index.repository.HackagePackage
import repository.Extensions._
import codesearch.core.index.directory.Directory._

import sys.process._
import codesearch.core.model.{HackageTable, Version}
import codesearch.core.util.Helper
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

  override protected def mapCSearchOutput(uri: String): Option[CSearchResult] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      logger.warn(s"bad uri: $uri")
      None
    } else {
      val fullPath             = Path(elems.head).relativeTo(pwd).toString
      val pathSeq: Seq[String] = fullPath.split('/').drop(4) // drop "data/packages/x/1.0/"
      val nLine                = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          logger.warn(s"bad uri: $uri")
          None
        case Some(name) =>
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some(
            CSearchResult(name,
                          s"https://hackage.haskell.org/package/$name",
                          CodeSnippet(
                            remPath,
                            firstLine,
                            nLine.toInt - 1,
                            rows
                          )))
      }
    }
  }

  override protected implicit def executor: ExecutionContext = ec
}

object HaskellIndex {
  def apply()(implicit ec: ExecutionContext) = new HaskellIndex(ec)
}
