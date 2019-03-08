package codesearch.core.regex

import codesearch.core.regex.lexer._
import codesearch.core.regex.lexer.tokens._

object PreciseMatch {
  def apply(query: String): String = {
    val queryTokens: Seq[Token] = Tokenizer.parseStringWithSpecialSymbols(query)
    val preciseMatch: Seq[Token] = queryTokens.map {
      case literal @ Literal(_) => literal
      case token: Token         => Literal(token.repr)
    }
    StringAssembler(preciseMatch)
  }
}
