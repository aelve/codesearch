package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.RubyCindex
import doobie.util.transactor.Transactor

final class RubyIndexer[F[_]: Sync: ContextShift](
    xa: Transactor[F]
) extends SourcesIndexer[F](RubyCindex, "gem", xa)

object RubyIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): RubyIndexer[F] = new RubyIndexer(xa)
}
