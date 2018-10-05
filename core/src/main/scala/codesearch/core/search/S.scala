package codesearch.core.search
import cats.effect.IO
import codesearch.core.search.Searcher.CSearchPage

trait S {
  def search(
      query: String,
      insensitive: Boolean,
      precise: Boolean,
      sources: Boolean,
      page: Int
  ): IO[CSearchPage]
}
