package codesearch.core.index
import java.io.File

import ammonite.ops.pwd
import sys.process._

import scala.concurrent.Future

case class TargetPackage(
    name: String,
    version: String
)

trait SourceDownloader {

  val repoUrl: String

  private def packageUrl: String = {}

  def download(target: TargetPackage): Future[Int] = {
    val archive     = packageFileGZ(target)
    val destination = packageFileDir(target)
    downloadAndExtract(archive, destination)
  }

  private def downloadAndExtract(archive: File, destination: File) = Future {
    download(archive)
    extract(archive, destination)
  }

  private def download(toFile: File) =
    Seq("curl", "-o", toFile.getCanonicalPath, packageUrl) !!

  private def extract(from: File, to: File) = {
    to.mkdirs()
    Seq("tar", "-xvf", from.getCanonicalPath, "-C", to.getCanonicalPath) !!
  }
  private def packageFileGZ(tp: TargetPackage): File =
    (pwd / 'data / tp.name / tp.version / s"${tp.version}.tar.gz").toIO

  private def packageFileDir(tp: TargetPackage): File =
    (pwd / 'data / tp.name / tp.version / tp.version).toIO
}
