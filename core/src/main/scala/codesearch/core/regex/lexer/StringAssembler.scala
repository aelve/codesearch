package codesearch.core.regex.lexer

import codesearch.core.regex.lexer.tokens.Token

object StringAssembler {

  /**
    * String builder from tokens: Seq[Token], we unite value a tokens
    *
    * @param tokens sequence tokens, example: Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World"))
    * @return string from tokens, example: "Hello World"
    */
  def buildStringFromTokens(tokens: Seq[Token]) = tokens.map(_.repr).mkString
}
