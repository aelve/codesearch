package codesearch.core.index.regex.lexer

import codesearch.core.index.regex.lexer.tokens.Token

object RowPicker {
  /**
    * String builder from tokens
    *
    * @param tokens sequence tokens
    * @return string from tokens
    */
  def buildStringFromTokens(tokens: Seq[Token]) = tokens.map(_.repr).mkString
}
