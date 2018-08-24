package codesearch.core.index.repository
import java.io.File

import scala.concurrent.Future

trait Download[A <: SourcePackage] {
  def downloadSources(pack: A): Future[File]
}

object Download {
  def apply[A <: SourcePackage](implicit download: Download[A]): Download[A] = download
}
