package codesearch.core.index

import sys.process._
import java.net.URL

import ammonite.ops.{Path, pwd}
import codesearch.core.db.HackageDB
import codesearch.core.util.Helper
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import codesearch.core.model.Version
import org.slf4j.{Logger, LoggerFactory}

object HackageIndex extends Index with HackageDB {
  private val logger: Logger = LoggerFactory.getLogger(HackageIndex.getClass)

  private val INDEX_LINK: String = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE_GZ: Path = pwd / 'data / "index.tar.gz"
  private val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  override def updateIndex(): Unit = {
    logger.info("update index")

    val archive = INDEX_SOURCE_GZ.toIO
    val destination = INDEX_SOURCE_DIR.toIO

    archive.getParentFile.mkdirs()
    destination.mkdirs()

    new URL(INDEX_LINK) #> archive !!

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }

  override def getLastVersions: Map[String, Version] = {
    val indexDir = INDEX_SOURCE_DIR.toIO
    val packageNames = indexDir.listFiles.filter(_.isDirectory)

    val lastVersions = packageNames.flatMap(packagePath =>
      packagePath.listFiles.filter(_.isDirectory).map(versionPath =>
        (packagePath.getName, Version(versionPath.getName))
      )
    ).groupBy(_._1).mapValues(_.map(_._2).max)

    lastVersions
  }

  def contentByURI(uri: String): Option[(String, Result)] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      println(s"bad uri: $uri")
      None
    } else {
      val fullPath = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(8)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(verName) =>
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some((verName, Result(
            s"https://hackage.haskell.org/package/$verName/src/$remPath",
            firstLine,
            nLine.toInt - 1,
            rows
          )))
      }
    }
  }
}
