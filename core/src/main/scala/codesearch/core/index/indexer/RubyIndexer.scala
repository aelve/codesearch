package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.RubyCindex
import doobie.util.transactor.Transactor

object RubyIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): SourcesIndexer[F] = new SourcesIndexer(RubyCindex, "gem", xa)
}
