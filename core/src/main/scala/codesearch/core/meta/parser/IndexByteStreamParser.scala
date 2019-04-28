package codesearch.core.meta.parser

import codesearch.core.db.repository.PackageIndexTableRow
import fs2.Stream

trait IndexByteStreamParser[F[_]] {
  def parse(stream: Stream[F, Byte]): F[Stream[F, PackageIndexTableRow]]
}
