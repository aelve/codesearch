package codesearch.core.regex.lexer

import codesearch.core.regex.lexer.tokens.Token

object RowPicker {

  /**
    * String builder from tokens
    *
    * @param tokens sequence tokens
    * @return string from tokens
    */
  def buildStringFromTokens(tokens: Seq[Token]) = tokens.map(_.repr).mkString
}
