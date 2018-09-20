package codesearch.core.index.repository

import java.io.File
import java.net.URLEncoder
import java.nio.file.Path

import codesearch.core.index.SupportedLangs.{Haskell, JavaScript, Ruby, Rust}
import codesearch.core.index.directory.Extractor
import com.softwaremill.sttp.{Uri, _}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.CompressionType.GZIP
import org.rauschig.jarchivelib.ArchiverFactory

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def url: Uri
}

private[index] final case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage with Haskell {
  val url: Uri = uri"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz"
}

private[index] final case class GemPackage(
    name: String,
    version: String
) extends SourcePackage with Ruby {
  val url: Uri = uri"https://rubygems.org/downloads/$name-$version.gem"
  override def unzipUsingMethod(from: File, to: Path): Unit = {
    val destDir    = to.toFile
    val allowedSet = Set("tgz", "tar.gz")
    ArchiverFactory.createArchiver(TAR).extract(from, destDir)
    destDir.listFiles
      .filter(file => allowedSet.exists(file.getName.toLowerCase.endsWith))
      .foreach(file => ArchiverFactory.createArchiver(TAR, GZIP).extract(file, destDir))
  }
}

private[index] final case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage with Rust {
  val url: Uri = uri"https://crates.io/api/v1/crates/$name/$version/download"
}

private[index] final case class NpmPackage(
    rawName: String,
    version: String
) extends SourcePackage with JavaScript {
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val name: String = URLEncoder.encode(rawName, "UTF-8")
  val url: Uri     = uri"https://registry.npmjs.org/$name/-/$name-$version.tgz"
}
