package codesearch.core.meta.unarchiver

import java.nio.file.Path

import codesearch.core.db.repository.PackageIndexTableRow
import fs2.Stream

private[meta] trait StreamIndexUnarchiver[F[_]] {
  def unarchiveToStream(path: Path): F[Stream[F, PackageIndexTableRow]]
}
