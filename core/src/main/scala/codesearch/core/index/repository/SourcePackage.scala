package codesearch.core.index.repository

import java.io.File
import java.net.URLEncoder
import java.nio.file.{Path, Paths}

import com.softwaremill.sttp._
import codesearch.core.util.Helper
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

trait PackageDirectory {
  val root: Path = Paths.get("./data")
  def fsArchivePath(lang: String): Path = Paths.get(
    s"${root.toFile.getCanonicalPath}/$lang/"
  )
}

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def fsArchivePath: Path
  def fsUnzippedPath: Path = Paths.get(s"${fsArchivePath.getParent.toFile.getCanonicalPath}/$name-$version")
  def url: Uri
  def extensions: Set[String] = Set.empty
}

case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path = Paths.get(s"./data/hackage/$name/$version/$name-$version.tgz")
  val url: Uri            = uri"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz"
}

case class GemPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path              = Paths.get(s"./data/gem/$name/$version/$name-$version.gem")
  val url: Uri                         = uri"https://rubygems.org/downloads/$name-$version.gem"
  override def extensions: Set[String] = Helper.langByExt.keySet
  override def unzippingMethod(from: File, to: File): Unit =
    ArchiverFactory.createArchiver(TAR).extract(from, to)
}

case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path = Paths.get(s"./data/crates/$name/$version/$name-$version.tgz")
  val url: Uri            = uri"https://crates.io/api/v1/crates/$name/$version/download"
}

case class NpmPackage(
    rawName: String,
    version: String
) extends SourcePackage {
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val name: String                     = URLEncoder.encode(rawName, "UTF-8")
  val fsArchivePath: Path              = Paths.get(s"./data/npm/$name/$version/$name-$version.tgz")
  val url: Uri                         = uri"https://registry.npmjs.org/$name/-/$name-$version.tgz"
  override def extensions: Set[String] = Set("js", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")
}
