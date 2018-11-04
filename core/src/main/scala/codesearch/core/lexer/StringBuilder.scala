package codesearch.core.lexer

import codesearch.core.lexer.tokens.Token

object StringBuilder {
  def buildStringFromTokens(tokens: Seq[Token]) = tokens.map(_.repr).mkString
}
