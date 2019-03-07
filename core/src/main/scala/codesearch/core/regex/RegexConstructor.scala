package codesearch.core.regex

import codesearch.core.regex.space.SpaceInsensitive

import scala.util.matching.Regex

object RegexConstructor {
  def apply(query: String, insensitive: Boolean, space: Boolean, precise: Boolean): Regex = {
    val preciseMatch     = if (precise) PreciseMatch(query) else query
    val spaceInsensitive = if (space) SpaceInsensitive.spaceInsensitiveString(preciseMatch) else preciseMatch
    val insensitiveCase  = if (insensitive) "(?i)" else ""
    s"$insensitiveCase$spaceInsensitive".r
  }
}
