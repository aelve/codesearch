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
    page: Int,
    callURI: String
)

object SearchRequest {
  def applyRaw(
      lang: String,
      query: String,
      filter: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = {
    val callURI: String = filter match {
      case Some(filter) =>
        s"/$lang/search?query=$query&filter=$filter&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
      case None =>
        s"/$lang/search?query=$query&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
    }

    SearchRequest(
      query,
      filter,
      isEnabled(insensitive),
      isEnabled(spaceInsensitive),
      isEnabled(preciseMatch),
      isEnabled(sourcesOnly),
      page.toInt,
      callURI
    )
  }

  private def isEnabled(param: String): Boolean = param == "on"
}
