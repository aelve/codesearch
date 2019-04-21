package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.RustCindex
import doobie.util.transactor.Transactor

final class RustIndexer[F[_]: Sync: ContextShift](
    xa: Transactor[F]
) extends SourcesIndexer[F](RustCindex, "crates", xa)

object RustIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): RustIndexer[F] = new RustIndexer(xa)
}
