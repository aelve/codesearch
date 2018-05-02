package codesearch.core.utilities

import java.io.{File, IOException}

import ammonite.ops.pwd
import codesearch.core.db.HackageDB
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}
import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class SourcesUtility {
}

object SourcesUtility {
  private val logger: Logger = LoggerFactory.getLogger(SourcesUtility.getClass)

  def update(): Unit = {
    val lastVersions = VersionsUtility.getLastVersions.mapValues(_.verString)

    logger.info(s"old index: (size = ${HackageDB.getSize}, updated = ${HackageDB.updated})")
    logger.info(s"new size of the index: ${lastVersions.size}")

    val futureAction = HackageDB.verNames().map { packages =>
      val packagesMap = Map(packages: _*)
      lastVersions.collect {
        case (packageName, currentVersion) if !packagesMap.get(packageName).contains(currentVersion) =>
          downloadSources(packageName, currentVersion)
      }
    }

    Await.result(futureAction, Duration.Inf)

  }

  def downloadSources(name: String, ver: String): Future[Int] = {
    logger.info(s"downloading package $name")

    val packageURL =
      s"https://hackage.haskell.org/package/$name-$ver/$name-$ver.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver / ver

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    destination.mkdirs()

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    try {
      downloadFile(packageURL, archive)
      logger.info(s"downloaded")

      archiver.extract(archive, destination)
      logger.info("extacted")

      val future = HackageDB.insertOrUpdate(name, ver)
      logger.info("DB updated")

      future
    } catch {
      case e: IOException =>
        Future[Int] {
          logger.info(e.getMessage)
          0
        }
    }
  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
    s"curl -o ${dstFile.getPath} $srcURL" !!
  }
}
