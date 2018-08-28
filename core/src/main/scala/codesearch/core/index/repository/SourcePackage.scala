package codesearch.core.index.repository

import java.io.File
import java.net.URLEncoder
import java.nio.file.{Path}

import com.softwaremill.sttp._
import com.softwaremill.sttp.Uri
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.CompressionType.GZIP
import org.rauschig.jarchivelib.ArchiverFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Extractor {
  def unzippingMethod(from: File, to: File): Unit =
    ArchiverFactory.createArchiver(TAR, GZIP).extract(from, to)
  def extract(archive: File, directory: Path): Future[File] =
    Future {
      val extractedDir = directory.toFile
      extractedDir.mkdirs()
      unzippingMethod(archive, extractedDir)
      extractedDir
    }
}

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def url: Uri
}

case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage with Haskell {
  val url: Uri = uri"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz"
}

case class GemPackage(
    name: String,
    version: String
) extends SourcePackage with Ruby {
  val url: Uri = uri"https://rubygems.org/downloads/$name-$version.gem"
  override def unzippingMethod(from: File, to: File): Unit =
    ArchiverFactory.createArchiver(TAR).extract(from, to)
}

case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage with Rust {
  val url: Uri = uri"https://crates.io/api/v1/crates/$name/$version/download"
}

case class NpmPackage(
    rawName: String,
    version: String
) extends SourcePackage with JavaScript {
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val name: String = URLEncoder.encode(rawName, "UTF-8")
  val url: Uri     = uri"https://registry.npmjs.org/$name/-/$name-$version.tgz"
}
