package codesearch.core.index.indexer

import cats.effect.{ContextShift, Sync}
import codesearch.core.index.directory.СindexDirectory.JavaScriptCindex
import doobie.util.transactor.Transactor

final class JavaScriptIndexer[F[_]: Sync: ContextShift](
    xa: Transactor[F]
) extends SourcesIndexer[F](JavaScriptCindex,  "npm", xa)

object JavaScriptIndexer {
  def apply[F[_]: Sync: ContextShift](
      xa: Transactor[F]
  ): JavaScriptIndexer[F] = new JavaScriptIndexer(xa)
}
