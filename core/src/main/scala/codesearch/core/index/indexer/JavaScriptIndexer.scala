package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.JavaScriptCindex
import doobie.util.transactor.Transactor

object JavaScriptIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): SourcesIndexer[F] = new SourcesIndexer[F](JavaScriptCindex, "npm", xa)
}
