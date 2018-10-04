package codesearch.core.search

import codesearch.core.index.{Haskell, JavaScript, Ruby, Rust}

sealed class SearchRequest(
    val query: String,
    val insensitive: Boolean,
    val preciseMatch: Boolean,
    val sourcesOnly: Boolean,
    val page: Int
)

case class JsSearchRequest(
    override val query: String,
    override val insensitive: Boolean,
    override val preciseMatch: Boolean,
    override val sourcesOnly: Boolean,
    override val page: Int
) extends SearchRequest(
      query,
      insensitive,
      preciseMatch,
      sourcesOnly,
      page
    ) with JavaScript

case class HaskellSearchRequest(
    override val query: String,
    override val insensitive: Boolean,
    override val preciseMatch: Boolean,
    override val sourcesOnly: Boolean,
    override val page: Int
) extends SearchRequest(
      query,
      insensitive,
      preciseMatch,
      sourcesOnly,
      page
    ) with Haskell

case class RubySearchRequest(
    override val query: String,
    override val insensitive: Boolean,
    override val preciseMatch: Boolean,
    override val sourcesOnly: Boolean,
    override val page: Int
) extends SearchRequest(
      query,
      insensitive,
      preciseMatch,
      sourcesOnly,
      page
    ) with Ruby

case class RustSearchRequest(
    override val query: String,
    override val insensitive: Boolean,
    override val preciseMatch: Boolean,
    override val sourcesOnly: Boolean,
    override val page: Int
) extends SearchRequest(
      query,
      insensitive,
      preciseMatch,
      sourcesOnly,
      page
    ) with Rust
