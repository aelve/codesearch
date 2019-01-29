package codesearch.core.search

import com.softwaremill.sttp._

/**
  * @param query input regular expression
  * @param filter filter for query
  * @param specialPath field to specify the special path
  * @param insensitive insensitive flag
  * @param spaceInsensitive space insensitive search flag
  * @param preciseMatch precise match flag
  * @param sourcesOnly sources only flag
  * @param page next pagination
  */
case class SearchRequest(
    query: String,
    filter: Option[String],
    specialPath: Option[String],
    insensitive: Boolean,
    spaceInsensitive: Boolean,
    preciseMatch: Boolean,
    sourcesOnly: Boolean,
    page: Int,
    callURI: Uri
)

object SearchRequest {
  def applyRaw(
      host: String,
      lang: String,
      query: String,
      filter: Option[String],
      specialPath: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      page: String
  ): SearchRequest = {
    val callURI: Uri =
      uri"$host/$lang/search?query=$query&filter=$filter&specialPath=$specialPath&insensitive=$insensitive&space=$spaceInsensitive&precise=$preciseMatch&sources=$sourcesOnly"

    SearchRequest(
      query,
      filter,
      specialPath,
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
