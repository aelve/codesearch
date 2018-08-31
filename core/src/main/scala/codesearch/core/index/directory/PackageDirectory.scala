package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.directory.PathOps._
import codesearch.core.index.repository._

object PathOps {
  implicit final class RichPath(val parent: Path) {
    def /(child: Path): Path   = Paths.get(parent.toFile.getPath, child.toFile.getPath)
    def /(child: String): Path = Paths.get(parent.toFile.getPath, child)
  }
}

private[index] trait Directory[A <: SourcePackage] {

  /** Defines root directory for sources storing
    *
    * @return root directory for sources storing
    */
  def sourcesDir: Path = Paths.get(s"./data")

  /** Defines extension of archive file
    *
    * @return extension of archive file
    */
  def extension: String = "tgz"

  /** Return path of archive file
    *
    * @param pack is instance of inheritor [[SourcePackage]]
    * @return path of archive file
    */
  def archive(pack: A): Path

  /** Return directory for unarchived files and dirs
    *
    * @param pack is instance of inheritor [[SourcePackage]]
    * @return directory for unarchived files and dirs
    */
  def unarchived(pack: A): Path = archive(pack).getParent
}

/** Companion contained defines type-classes for inheritors [[SourcePackage]] */
object PackageDirectory {

  implicit class PackageDirectoryOps[A <: SourcePackage](val pack: A) {
    def archive(implicit ev: Directory[A]): Path    = ev.archive(pack)
    def unarchived(implicit ev: Directory[A]): Path = ev.unarchived(pack)
  }

  implicit def hackageDirectory: Directory[HackagePackage] = new Directory[HackagePackage] {
    override def archive(pack: HackagePackage): Path =
      sourcesDir / "hackage" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def npmDirectory: Directory[NpmPackage] = new Directory[NpmPackage] {
    override def archive(pack: NpmPackage): Path =
      sourcesDir / "npm" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def gemDirectory: Directory[GemPackage] = new Directory[GemPackage] {
    override def extension: String = "gem"
    override def archive(pack: GemPackage): Path =
      sourcesDir / "gem" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }

  implicit def sourceDirectory: Directory[CratesPackage] = new Directory[CratesPackage] {
    override def archive(pack: CratesPackage): Path =
      sourcesDir / "crates" / s"${pack.name}" / s"${pack.version}" / s"${pack.name}-${pack.version}.$extension"
  }
}
