package codesearch.core.regex

import codesearch.core.regex.lexer._
import codesearch.core.regex.lexer.tokens._
import codesearch.core.regex.space.SpaceInsensitive

import scala.util.matching.Regex

object RegexHelper {
  def preciseMatch(query: String): String = {
    val queryTokens: Seq[Token] = Tokenizer.parseStringWithSpecialSymbols(query)
    val preciseMatch: Seq[Token] = queryTokens.map {
      case literal @ Literal(_)    => literal
      case space @ Space(_)        => space
      case escaped @ Escaped("\\") => Escaped("\\" + escaped.repr)
      case token: Token            => Escaped(token.repr)
    }
    StringAssembler.buildStringFromTokens(preciseMatch)
  }

  def buildRegex(query: String, insensitive: Boolean, space: Boolean, precise: Boolean): Regex = {
    val preciseMatch     = if (precise) RegexHelper.preciseMatch(query) else query
    val spaceInsensitive = if (space) SpaceInsensitive.spaceInsensitiveString(preciseMatch) else preciseMatch
    val insensitiveCase  = if (insensitive) "(?i)" else ""
    s"$insensitiveCase$spaceInsensitive".r
  }
}
