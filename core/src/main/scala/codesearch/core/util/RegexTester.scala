package codesearch.core.util

import scala.util.matching.Regex

object RegexTester {

  def regexIsInvalid(regex: String, line: String): Boolean = {
    try {
      regex.r.findAllMatchIn(line)
      false
    } catch {
      case x: java.util.regex.PatternSyntaxException => true
    }
  }

}
