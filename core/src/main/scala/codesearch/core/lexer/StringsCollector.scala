package codesearch.core.lexer

import codesearch.core.lexer.tokens.Token

object StringsCollector {
  def collectStringFromTokens(tokens: Seq[Token]) = tokens.map(_.repr).mkString
}
