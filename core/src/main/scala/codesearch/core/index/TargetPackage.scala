package codesearch.core.index

import java.net.URI
import java.nio.file.{Path, Paths}

trait TargetPackage {
  def fsPath: Path
  def url: URI
  def extentions: Seq[String]
}

case class HackagePackage(
    name: String,
    version: String,
    extentions: Seq[String]
) extends TargetPackage {
  val fsPath: Path = Paths.get(new URI(s"./hackage/$name/$version"))
  val url: URI     = URI.create(s"https://hackage.haskell.org/package/$name-$version/$name-$version.tar.gz")
}

case class NpmPackage(
    name: String,
    version: String,
    extentions: Seq[String]
)
