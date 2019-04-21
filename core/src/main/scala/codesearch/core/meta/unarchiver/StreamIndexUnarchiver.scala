package codesearch.core.meta.unarchiver

import codesearch.core.db.repository.PackageIndex
import fs2.Pipe

trait StreamIndexUnarchiver[F[_]] {
  def packages: Pipe[F, Byte, PackageIndex]
}
