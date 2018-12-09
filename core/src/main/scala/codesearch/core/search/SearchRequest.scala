package codesearch.core.search

/**
  * @param query input regular expression
  * @param insensitive insensitive flag
  * @param spaceInsensitive insensitive search flag
  * @param preciseMatch precise match flag
  * @param sourcesOnly sources only flag
  * @param page next pagination
  */
case class SearchRequest(
    query: String,
    insensitive: Boolean,
    spaceInsensitive: Boolean,
    preciseMatch: Boolean,
    sourcesOnly: Boolean,
    page: Int
)

object SearchRequest {
  def applyRaw(
      query: String,
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = SearchRequest(
    query,
    isEnabled(insensitive),
    isEnabled(spaceInsensitive),
    isEnabled(preciseMatch),
    isEnabled(sourcesOnly),
    page.toInt
  )
  private def isEnabled(param: String): Boolean = param == "on"
}
