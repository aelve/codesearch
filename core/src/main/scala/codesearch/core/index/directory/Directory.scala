package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

import codesearch.core.index.directory.Preamble._
import codesearch.core.index.repository._
import simulacrum.typeclass

@typeclass trait Directory[A] {

  /** Defines path to directory with source files */
  def packageDir(pack: A): Path

  /** Defines extension of archive file */
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
object Directory {

  /** Defines root directory for sources storing */
  val sourcesDir: Path = Paths.get("./data/packages")

  implicit def hackageDirectory: Directory[HackagePackage] = new Directory[HackagePackage] {
    override def archive(pack: HackagePackage): Path =
      packageDir(pack) / s"${pack.name}-${pack.version}.$extension"

    override def packageDir(pack: HackagePackage): Path =
      sourcesDir / "hackage" / pack.name / pack.version
  }

  implicit def npmDirectory: Directory[NpmPackage] = new Directory[NpmPackage] {
    override def archive(pack: NpmPackage): Path =
      packageDir(pack) / s"${pack.encodedName}-${pack.version}.$extension"

    override def packageDir(pack: NpmPackage): Path =
      sourcesDir / "npm" / pack.encodedName / pack.version
  }

  implicit def gemDirectory: Directory[GemPackage] = new Directory[GemPackage] {
    override def extension: String = "gem"

    override def archive(pack: GemPackage): Path =
      packageDir(pack) / s"${pack.name}-${pack.version}.$extension"

    override def packageDir(pack: GemPackage): Path =
      sourcesDir / "gem" / pack.name / pack.version
  }

  implicit def sourceDirectory: Directory[CratesPackage] = new Directory[CratesPackage] {
    override def archive(pack: CratesPackage): Path =
      packageDir(pack) / s"${pack.name}-${pack.version}.$extension"

    override def packageDir(pack: CratesPackage): Path =
      sourcesDir / "crates" / pack.name / pack.version
  }
}
