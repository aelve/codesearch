package codesearch.core.index

import java.net.URL

import ammonite.ops.{Path, pwd}
import codesearch.core.db.HackageDB

import sys.process._
import codesearch.core.model.{HackageTable, Version}
import codesearch.core.util.Helper
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

case class Result(fileLink: String, firstLine: Int, nLine: Int, ctxt: Seq[String])
case class PackageResult(name: String, packageLink: String, results: Seq[Result])

class HaskellIndex(val ec: ExecutionContext) extends LanguageIndex[HackageTable] with HackageDB {

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".hackage_csearch_index"
  override protected val langExts: String  = ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$"

  private val INDEX_LINK: String     = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE_GZ: Path  = pwd / 'data / "index.tar.gz"
  private val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  def csearch(searchQuery: String,
              insensitive: Boolean,
              precise: Boolean,
              sources: Boolean,
              page: Int): (Int, Seq[PackageResult]) = {
    val answers = runCsearch(searchQuery, insensitive, precise, sources)
    (answers.length,
     answers
       .slice(math.max(page - 1, 0) * 100, page * 100)
       .flatMap(contentByURI)
       .groupBy { x =>
         (x._1, x._2)
       }
       .map {
         case ((verName, packageLink), results) =>
           PackageResult(verName, packageLink, results.map(_._3).toSeq)
       }
       .toSeq
       .sortBy(_.name))
  }

  override protected def downloadSources(name: String, ver: String): Future[Int] = {
    logger.info(s"downloading package $name")

    val packageURL =
      s"https://hackage.haskell.org/package/$name-$ver/$name-$ver.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver

    archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir)
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

  private def contentByURI(uri: String): Option[(String, String, Result)] = {
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
            (name,
             s"https://hackage.haskell.org/package/$name",
             Result(
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
