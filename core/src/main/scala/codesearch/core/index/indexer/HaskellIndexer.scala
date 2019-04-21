package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.HaskellCindex
import doobie.util.transactor.Transactor

final class HaskellIndexer[F[_]: Sync: ContextShift](
    xa: Transactor[F]
) extends SourcesIndexer[F](HaskellCindex, "hackage", xa)

object HaskellIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): HaskellIndexer[F] = new HaskellIndexer(xa)
}
