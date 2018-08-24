package codesearch.core.index

import java.io.File
import java.net.URI
import java.nio.file.{Path, Paths}

import codesearch.core.util.Helper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sys.process._

trait Extractor {
  def extractor(archive: File, directory: File): Future[File] =
    Future {
      Seq("tar", "-xvf", archive.getCanonicalPath, "-C", directory.getCanonicalPath) !!;
      directory
    }
}

trait SourcePackage extends Extractor {
  def fsPath: Path
  def url: URI
  def extentions: Set[String] = Set.empty
}

case class HackagePackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsPath: Path = Paths.get(new URI(s"./hackage/$name/$version"))
  val url: URI     = URI.create(s"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz")
}

case class GemPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsPath: Path                     = Paths.get(new URI(s"./gem/$name/$version"))
  val url: URI                         = URI.create(s"https://rubygems.org/downloads/$name-$version.gem")
  override def extentions: Set[String] = Helper.langByExt.keySet
  override def extractor(archive: File, directory: File): Future[File] = {
    Future {
      Seq("gem", "unpack", s"--target=${directory.getCanonicalFile}", archive.getCanonicalFile) !!;
      directory
    }
  }
}

case class NpmPackage(
    name: String,
    version: String
) extends SourcePackage {
  val fsPath: Path                     = Paths.get(new URI(s"./hackage/$name/$version"))
  val url: URI                         = URI.create(s"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz")
  override def extentions: Set[String] = Set("js", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")
}

trait Downloader[A <: SourcePackage] {
  def downloadSources(pack: A)
}
