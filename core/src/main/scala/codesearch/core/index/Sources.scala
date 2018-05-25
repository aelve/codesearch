package codesearch.core.index

import java.io.File

import ammonite.ops.Path

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import org.apache.commons.io.FilenameUtils
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait Sources[VTable <: DefaultTable] {
  protected val indexAPI: Index with DefaultDB[VTable]

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

    futureAction
  }

  def archiveDownloadAndExtract(name: String, ver: String, packageURL: String,
                                packageFileGZ: Path,
                                packageFileDir: Path, extensions: Option[Set[String]] = None): Future[Int] = {
    val archive = packageFileGZ.toIO
    val destination = packageFileDir.toIO
    println(archive, "->", destination)

    Future {

      try {
        destination.mkdirs()

        val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
        downloadFile(packageURL, archive)

        archiver.extract(archive, destination)

        true
      } catch {
        case e: Exception =>
          e.printStackTrace()

          false
      }
    } map {
      case true =>
        extensions.isEmpty || applyFilter(extensions.get, destination) || applyFilter(extensions.get, archive)
      case false =>
        false
    } flatMap {
      case true =>
        indexAPI.insertOrUpdate(name, ver)
      case false =>
        Future {
          0
        }
    }
  }

  def applyFilter(extensions: Set[String], curFile: File): Boolean = {
    if (curFile.isDirectory) {
      curFile.listFiles.forall(applyFilter(extensions, _))
    } else {
      val ext = FilenameUtils.getExtension(curFile.getName)
      !curFile.exists || (extensions contains ext) || curFile.delete
    }
  }

  def downloadFile(srcURL: String, dstFile: File): Unit = {
    Seq("curl", "-o", dstFile.getAbsolutePath, srcURL) !!
//    s"curl -o ${dstFile.getPath} $srcURL" !!
  }

  def runCsearch(searchQuery: String,
                 insensitive: Boolean, precise: Boolean, pathRegex: String) = {
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
    (args #| Seq("head", "-1001")).!!
  }
}
