package codesearch.core.search

import cats.effect.IO
import codesearch.core.search.Searcher.CSearchPage

class HaskellSearch extends S {
  override def search(
      query: String,
      insensitive: Boolean,
      precise: Boolean,
      sources: Boolean,
      page: Int
  ): IO[CSearchPage] = new Search(HaskellSearchRequest(query, insensitive, precise, sources, page)).search
}
