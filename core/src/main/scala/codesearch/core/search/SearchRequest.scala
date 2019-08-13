package codesearch.core.search

import com.softwaremill.sttp._

/**
  * @param query input regular expression
  * @param filter filter for query
  * @param filePath field to specify the file path
  * @param insensitive insensitive flag
  * @param spaceInsensitive space insensitive search flag
  * @param preciseMatch precise match flag
  * @param sourcesOnly sources only flag
  * @param withoutTests search without tests
  * @param page next pagination
  */
case class SearchRequest(
    lang: String,
    query: String,
    filter: Option[String],
    filePath: Option[String],
    insensitive: Boolean,
    spaceInsensitive: Boolean,
    preciseMatch: Boolean,
    sourcesOnly: Boolean,
    withoutTests: Boolean,
    page: Int
) {

  /**
    * @param host host for building url
    * @return url for next page in pagination
    */
  def callURI(host: String): Uri = {
    def stringify(x: Boolean): String = if (x) "on" else "off"

    uri"$host/$lang/search?query=$query&filter=$filter&filePath=$filePath&insensitive=${stringify(insensitive)}&space=${stringify(
      spaceInsensitive)}&precise=${stringify(preciseMatch)}&sources=${stringify(sourcesOnly)}&withoutTests=${stringify(withoutTests)}"
  }
}

object SearchRequest {
  def applyRaw(
      lang: String,
      query: String,
      filter: Option[String],
      filePath: Option[String],
      insensitive: String,
      spaceInsensitive: String,
      preciseMatch: String,
      sourcesOnly: String,
      withoutTests: String,
      page: String
  ): SearchRequest = {
    SearchRequest(
      lang,
      clean(query),
      filter.map(clean),
      filePath.map(clean),
      isEnabled(insensitive),
      isEnabled(spaceInsensitive),
      isEnabled(preciseMatch),
      isEnabled(sourcesOnly),
      isEnabled(withoutTests),
      page.toInt,
    )
  }

  private def clean(string: String): String =
    string.trim.replaceAll("[[\\x00-\\x1F\\x7F]&&[^\\r\\n\\t]]", "")

  private def isEnabled(param: String): Boolean = param == "on"
}
