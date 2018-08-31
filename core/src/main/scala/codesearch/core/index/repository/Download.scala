package codesearch.core.index.repository

import java.nio.file.Path

import scala.concurrent.Future

trait Download[A <: SourcePackage] {
  def downloadSources(pack: A): Future[Path]
}

object Download {
  def apply[A <: SourcePackage](implicit download: Download[A]): Download[A] = download
}
