package codesearch.core.index.repository

import java.io.File
import java.net.URLEncoder

import codesearch.core.index.directory.Extractor
import com.softwaremill.sttp.{Uri, _}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def url: Uri
}

final case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage with Haskell {
  val url: Uri = uri"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz"
}

final case class GemPackage(
    name: String,
    version: String
) extends SourcePackage with Ruby {
  val url: Uri = uri"https://rubygems.org/downloads/$name-$version.gem"
  override def unzippingMethod(from: File, to: File): Unit =
    ArchiverFactory.createArchiver(TAR).extract(from, to)
}

final case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage with Rust {
  val url: Uri = uri"https://crates.io/api/v1/crates/$name/$version/download"
}

final case class NpmPackage(
    rawName: String,
    version: String
) extends SourcePackage with JavaScript {
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val name: String = URLEncoder.encode(rawName, "UTF-8")
  val url: Uri     = uri"https://registry.npmjs.org/$name/-/$name-$version.tgz"
}