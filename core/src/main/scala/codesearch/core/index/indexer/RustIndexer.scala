package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.RustCindex
import doobie.util.transactor.Transactor

object RustIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): SourcesIndexer[F] = new SourcesIndexer(RustCindex, "crates", xa)
}
