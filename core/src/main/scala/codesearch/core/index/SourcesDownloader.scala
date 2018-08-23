package codesearch.core.index
import java.io.File

import ammonite.ops.pwd
import sys.process._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class NpmPackage(ts: TargetPackage)     extends AnyVal

trait Download[T] {
  def downloadSources: Future[File]
}

trait Extract[T <: TargetPackage] {
  def extractSources: Future[File]
}

trait SourcesDownloader {

  val repoUrl: String

  private def packageUrl(target: TargetPackage): String = {}

  def download(target: TargetPackage): Future[Int] = {
    val archive     = packageFileGZ(target)
    val destination = packageFileDir(target)
    downloadAndExtract(archive, destination)
  }

  def packageFileGZ(tp: TargetPackage): File =
    (pwd / 'data / tp.repository / tp.name / tp.version / s"${tp.version}.tar.gz").toIO

  def packageFileDir(tp: TargetPackage): File =
    (pwd / 'data / tp.repository / tp.name / tp.version / tp.version).toIO

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
}
