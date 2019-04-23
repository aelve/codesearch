package codesearch.core.meta.parser

import codesearch.core.db.repository.PackageIndex
import fs2.Stream

trait IndexByteStreamParser[F[_]] {
  def parse(stream: Stream[F, Byte]): F[Stream[F, PackageIndex]]
}
