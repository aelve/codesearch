package codesearch.core.regex

import codesearch.core.regex.space.SpaceInsensitiveString

object RegexConstructor {
  def apply(query: String, insensitive: Boolean, space: Boolean, precise: Boolean): String = {
    val preciseMatch     = if (precise) PreciseMatch(query) else query
    val spaceInsensitive = if (space) SpaceInsensitiveString(preciseMatch) else preciseMatch
    val insensitiveCase  = if (insensitive) "(?i)" else ""
    s"$insensitiveCase$spaceInsensitive"
  }
}
