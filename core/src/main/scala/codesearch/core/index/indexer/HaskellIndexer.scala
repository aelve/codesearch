package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.HaskellCindex
import doobie.util.transactor.Transactor

object HaskellIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): SourcesIndexer[F] = new SourcesIndexer(HaskellCindex, "hackage", xa)
}
