package codesearch.core.index

import java.io.File

import ammonite.ops.Path

import sys.process._
import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable
import codesearch.core.util.Helper
import org.apache.commons.io.FilenameUtils

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

    Future {
      val archive = packageFileGZ.toIO
      val destination = packageFileDir.toIO

      try {
        destination.mkdirs()
        println(s"::::::START::::::::$name-$ver")

//        val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
        (Seq("curl", "-o", archive.getCanonicalPath, packageURL) #&&
          Seq("tar", "-I", "pigz", "-xvfz", archive.getCanonicalPath, "-C", destination.getCanonicalPath)
          ) !!


//        archiver.extract(archive, destination)

        true
      } catch {
        case e: Exception =>
          println(s"::::::FAILED::::::::$name-$ver")
          e.printStackTrace()

          false
      }
    } flatMap {
      case true =>
        val archive = packageFileGZ.toIO
        val destination = packageFileDir.toIO

        if (extensions.isDefined) {
          applyFilter(extensions.get, archive)
          applyFilter(extensions.get, destination)
        }

        indexAPI.insertOrUpdate(name, ver)
      case false =>
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
