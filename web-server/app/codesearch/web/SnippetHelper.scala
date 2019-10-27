package codesearch.web

import play.twirl.api.{Html, HtmlFormat}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
  * @author sss3 (Vladimir Alekseev)
  */
object SnippetHelper {

  def markAllMatches(line: String, matches: Iterator[Regex.Match]): List[Html] = {
    var lastL                         = 0
    var lastR                         = 0
    val htmlBuilder: ListBuffer[Html] = mutable.ListBuffer.empty[Html]

    matches.map(m => (m.start, m.end)).filter { case (l, r) => r > l }.foreach {
      case (l, r) =>
        if (lastR < l || l == 0) {
          if (lastR != 0) {
            htmlBuilder.append(Html("</mark>"))
          }
          htmlBuilder.append(HtmlFormat.escape(line.substring(lastR, l)))
          htmlBuilder.append(Html("<mark>"))
          htmlBuilder.append(HtmlFormat.escape(line.substring(l, r)))
          lastL = l
          lastR = r
        } else {
          htmlBuilder.append(HtmlFormat.escape(line.substring(lastR, math.max(lastR, r))))
          lastR = math.max(lastR, r)
        }
    }
    if (lastR != 0) {
      htmlBuilder.append(Html("</mark>"))
    }
    htmlBuilder.append(HtmlFormat.escape(line.substring(lastR)))
    htmlBuilder.append(HtmlFormat.raw("\n"))
    htmlBuilder.toList
  }
}
