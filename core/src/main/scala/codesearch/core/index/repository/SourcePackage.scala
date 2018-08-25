package codesearch.core.index.repository

import sys.process._
import java.io.File
import java.net.{URI, URLEncoder}
import java.nio.file.{Path, Paths}

import codesearch.core.util.Helper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Extractor {
  def unzippingMethod(from: String, to: String): Unit =
    Seq("tar", "-xvf", from, "-C", to) !!
  def extract(archive: File, directory: Path): Future[File] =
    Future {
      val extractedDir = directory.toFile
      extractedDir.mkdirs()
      extractedDir
        .listFiles()
        .filter(_.isFile)
        .foreach(file => {
          unzippingMethod(archive.getCanonicalPath, file.getCanonicalPath)
        })
      extractedDir
    }
}

trait SourcePackage extends Extractor {
  val name: String
  val version: String
  def fsArchivePath: Path
  def fsUnzippedPath: Path = Paths.get(s"${fsArchivePath.toFile.getPath}/$name-$version")
  def url: URI
  def extensions: Set[String] = Set.empty
}

case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path = Paths.get(s"./data/hackage/$name/$version")
  val url: URI            = URI.create(s"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz")
}

case class GemPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path              = Paths.get(s"./data/gem/$name/$version")
  val url: URI                         = URI.create(s"https://rubygems.org/downloads/$name-$version.gem")
  override def extensions: Set[String] = Helper.langByExt.keySet
  override def unzippingMethod(from: String, to: String): Unit =
    Seq("gem", "unpack", s"--target=$to", from) !!
}

case class CratesPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsArchivePath: Path = Paths.get(s"./data/crates/$name/$version")
  val url: URI            = URI.create(s"https://crates.io/api/v1/crates/$name/$version/download")
}

case class NpmPackage(
    rawName: String,
    version: String
) extends SourcePackage {
  //Because package name can look like: react>>=native@@router!!v2.1.123(refactored:-))
  val name: String                     = URLEncoder.encode(rawName, "UTF-8")
  val fsArchivePath: Path              = Paths.get(s"./data/npm/$name/$version")
  val url: URI                         = URI.create(s"https://registry.npmjs.org/$name/-/$name-$version.tgz")
  override def extensions: Set[String] = Set("js", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")
}
