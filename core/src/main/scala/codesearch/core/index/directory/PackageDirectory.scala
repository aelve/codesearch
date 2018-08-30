package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.directory.PathOps._
import codesearch.core.index.repository._

object PathOps {
  implicit final class RichPath(val parent: Path) {
    def /(child: Path): Path   = Paths.get(s"${parent.toFile.getPath}/${child.toFile.getPath}")
    def /(child: String): Path = Paths.get(s"${parent.toFile.getPath}/$child")
  }
}

trait Directory[A <: SourcePackage] {
  def root: Path        = Paths.get(s"./data")
  def extension: String = "tgz"
  def archive(pack: A): Path
  def unarchived(pack: A): Path = archive(pack).getParent
}

object PackageDirectory {

  implicit class PackageDirectoryOps[A <: SourcePackage](val pack: A) {
    def archive(implicit ev: Directory[A]): Path    = ev.archive(pack)
    def unarchived(implicit ev: Directory[A]): Path = ev.unarchived(pack)
  }

  implicit def hackageDirectory: Directory[HackagePackage] = new Directory[HackagePackage] {
    override def archive(pack: HackagePackage): Path =
      root / "hackage" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def npmDirectory: Directory[NpmPackage] = new Directory[NpmPackage] {
    override def archive(pack: NpmPackage): Path =
      root / "npm" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def gemDirectory: Directory[GemPackage] = new Directory[GemPackage] {
    override def extension: String = "gem"
    override def archive(pack: GemPackage): Path =
      root / "gem" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def sourceDirectory: Directory[CratesPackage] = new Directory[CratesPackage] {
    override def archive(pack: CratesPackage): Path =
      root / "crates" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }
}
