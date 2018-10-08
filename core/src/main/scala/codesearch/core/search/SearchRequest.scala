package codesearch.core.search

case class SearchRequest(
    query: String,
    insensitive: Boolean,
    preciseMatch: Boolean,
    sourcesOnly: Boolean,
    page: Int
)

object SearchRequest {
  def applyRaw(
      query: String,
      insensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = SearchRequest(
    query,
    isEnabled(insensitive),
    isEnabled(preciseMatch),
    isEnabled(sourcesOnly),
    page.toInt
  )
  private def isEnabled(param: String): Boolean = param == "on"
}
