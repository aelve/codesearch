package codesearch.core.search

/**
  * @param query input regular expression
  * @param filter filter for query
  * @param insensitive insensitive flag
  * @param spaceInsensitive space insensitive search flag
  * @param preciseMatch precise match flag
  * @param sourcesOnly sources only flag
  * @param page next pagination
  */
case class SearchRequest(
    query: String,
    filter: Option[String],
    insensitive: Boolean,
    spaceInsensitive: Boolean,
    preciseMatch: Boolean,
    sourcesOnly: Boolean,
    page: Int
) {
  def callURI(
      lang: String,
      query: String,
      filter: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String
  ): String = filter match {
    case Some(filter) =>
      s"/$lang/search?query=$query&filter=$filter&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
    case None =>
      s"/$lang/search?query=$query&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
  }
}

object SearchRequest {
  def applyRaw(
      query: String,
      filter: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = SearchRequest(
    query,
    filter,
    isEnabled(insensitive),
    isEnabled(spaceInsensitive),
    isEnabled(preciseMatch),
    isEnabled(sourcesOnly),
    page.toInt
  )

  private def isEnabled(param: String): Boolean = param == "on"
}
