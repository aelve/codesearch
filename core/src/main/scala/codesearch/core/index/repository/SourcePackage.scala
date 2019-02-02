package codesearch.core.index.repository

import java.io.File
import java.net.URLEncoder
import java.nio.file.Path

import cats.effect.Sync
import codesearch.core.index.directory.Extractor
import com.softwaremill.sttp.{Uri, _}
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType.GZIP

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def url: Uri
}

private[index] final case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage {
  val url: Uri = uri"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz"
}

private[index] final case class GemPackage(
    name: String,
    version: String
) extends SourcePackage {
  val url: Uri = uri"https://rubygems.org/downloads/$name-$version.gem"
  override def unzipUsingMethod[F[_]](from: Path, to: Path)(implicit F: Sync[F]): F[Unit] = F.delay {
    val destDir    = to.toFile
    val allowedSet = Set("tgz", "tar.gz")
    ArchiverFactory.createArchiver(TAR).extract(from.toFile, destDir)
    destDir.listFiles
      .filter(file => allowedSet.exists(file.getName.toLowerCase.endsWith))
      .foreach(file => ArchiverFactory.createArchiver(TAR, GZIP).extract(file, destDir))
  }
  override def flatDir[F[_]](unarchived: Path)(implicit F: Sync[F]): F[Path] = F.pure(unarchived)
}

private[index] final case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage {
  val url: Uri = uri"https://crates.io/api/v1/crates/$name/$version/download"
}

private[index] final case class NpmPackage(
    name: String,
    version: String
) extends SourcePackage {
  private val urlString = s"https://registry.npmjs.org/$name/-/$name-$version.tgz"
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val encodedName: String = URLEncoder.encode(name, "UTF-8")
  val url: Uri            = uri"$urlString"
}
