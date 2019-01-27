package codesearch.core.search

/**
  * @param query input regular expression
  * @param filter filter for query
  * @param specify field to specify the path
  * @param insensitive insensitive flag
  * @param spaceInsensitive space insensitive search flag
  * @param preciseMatch precise match flag
  * @param sourcesOnly sources only flag
  * @param page next pagination
  */
case class SearchRequest(
    query: String,
    filter: Option[String],
    specifyPath: Option[String],
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
      specifyPath: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = {
    val callURI: String = (filter, specifyPath) match {
      case (Some(filter), Some(specifyPath)) =>
        s"/$lang/search?query=$query&filter=$filter&specifyPath=$specifyPath&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
      case (Some(filter), None) =>
        s"/$lang/search?query=$query&filter=$filter&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
      case (None, Some(specifyPath)) =>
        s"/$lang/search?query=$query&specifyPath=$specifyPath&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
      case (None, None) =>
        s"/$lang/search?query=$query&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"
    }

    SearchRequest(
      query,
      filter,
      specifyPath,
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
