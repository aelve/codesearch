package codesearch.core.index

import java.io.File

import ammonite.ops.{Path, root}

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait Sources[VTable <: DefaultTable] {
  protected val indexAPI: Index with DefaultDB[VTable]
  protected val logger: Logger
  protected val langExts: String

  protected val indexFile: String
  protected lazy val indexPath: Path = root / 'root / 'aelve / 'data / indexFile // FIXME

  def downloadSources(name: String, ver: String): Future[Int]

  implicit val ec: ExecutionContext = new ExecutionContext {

    import java.util.concurrent.Executors

    private val threadPool = Executors.newFixedThreadPool(100)

    override def execute(runnable: Runnable): Unit = {
      threadPool.submit(runnable)
    }

    override def reportFailure(cause: Throwable): Unit = {
      cause.printStackTrace()
    }
  }


  def update(): Future[Int] = {
    val lastVersions = indexAPI.getLastVersions.mapValues(_.verString)

    val futureAction = indexAPI.verNames().flatMap { packages =>
      val packagesMap = Map(packages: _*)

      Future.sequence(lastVersions.filter {
        case (packageName, currentVersion) =>
          !packagesMap.get(packageName).contains(currentVersion)
      }.map {
        case (packageName, currentVersion) =>
          downloadSources(packageName, currentVersion)
      })
    }.map(_.sum)

    Future {
      logger.debug("UPDATE PACKAGES")
    } flatMap { _ =>
      futureAction
    }
  }

  def archiveDownloadAndExtract(name: String, ver: String, packageURL: String,
                                packageFileGZ: Path,
                                packageFileDir: Path, extensions: Option[Set[String]] = None): Future[Int] = {

    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO

    try {
      destination.mkdirs()

      Seq("curl", "-o", archive.getCanonicalPath, packageURL) !!

      Seq("tar", "-xvf", archive.getCanonicalPath, "-C", destination.getCanonicalPath) !!

      if (extensions.isDefined) {
        applyFilter(extensions.get, archive)
        applyFilter(extensions.get, destination)
      }

      indexAPI.insertOrUpdate(name, ver)
    } catch {
      case e: Exception =>
        logger.debug(e.getLocalizedMessage)
        Future {
          0
        }
    }
  }

  def applyFilter(extensions: Set[String], curFile: File): Unit = {
    if (curFile.isDirectory) {
      curFile.listFiles.foreach(applyFilter(extensions, _))
    } else {
      val ext = FilenameUtils.getExtension(curFile.getName)
      if (curFile.exists() && !(extensions contains ext)) {
        curFile.delete
      }
    }
  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
//    s"curl -o ${dstFile.getPath} $srcURL" !!
  }

  def runCsearch(searchQuery: String,
                 insensitive: Boolean, precise: Boolean, sources: Boolean): String = {
    val pathRegex = {
      if (sources) {
        langExts
      } else {
        "*"
      }
    }

    val query: String = {
      if (precise) {
        Helper.hideSymbols(searchQuery)
      } else {
        searchQuery
      }
    }

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (insensitive) {
      args.append("-i")
    }
    args.append("-f", pathRegex)
    args.append(query)
    logger.debug(indexPath.toString())
    (Process(args, None, "CSEARCHINDEX" -> indexPath.toString()) #| Seq("head", "-1001")).!!
  }
}
