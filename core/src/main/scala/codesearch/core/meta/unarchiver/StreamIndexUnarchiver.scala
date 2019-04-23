package codesearch.core.meta.unarchiver

import java.nio.file.Path

import codesearch.core.db.repository.PackageIndex
import fs2.Stream

trait StreamIndexUnarchiver[F[_]] {
  def unarchive(path: Path): F[Stream[F, PackageIndex]]
}
