package codesearch.core.regex.lexer

import fastparse._
import NoWhitespace._
import codesearch.core.regex.lexer.tokens._

object Tokenizer {

  private def leftForOtherSymbols[_: P] = P(CharIn("(", "[", "[^")).!

  private def rightForOtherSymbols[_: P] = P(CharIn(")", "]")).!

  private def specialSymbols[_: P] =
    P(CharIn("\\\\", " ", "$", "%", "^", "&", "*", "+", "?", "!", "]", ")", "[", "[^", "(")).!

  private def parserEscaped[_: P] = P(CharIn("\\\\") ~ AnyChar.!).map(a => Escaped(a.charAt(0)))

  private def parserAnyStringInOtherSymbols[_: P] = P(!"\\" ~ !rightForOtherSymbols ~ AnyChar).rep.!

  private def parserOtherSymbols[_: P] =
    P(leftForOtherSymbols ~ parserAnyStringInOtherSymbols ~ rightForOtherSymbols).rep(1).!.map(Other(_))

  private def parserSpecialSymbol[_: P] =
    P(specialSymbols.map(specialSymbolInString => SpecialSymbol(specialSymbolInString)))

  private def parserAnyStringToSpecialSymbol[_: P] = P((!specialSymbols ~ AnyChar).rep(1).!.map(Literal(_)))

  private def parseStringWithSpecialSymbols[_: P] =
    P(parserEscaped | parserOtherSymbols | parserAnyStringToSpecialSymbol | parserSpecialSymbol).rep

  /**
    * Parse string into a Tokens
    *
    * @param query search query
    * @return sequence tokens
    */
  def parseStringWithSpecialSymbols(query: String): Seq[Token] = {
    val Parsed.Success(tokens, _) = parse(query, parseStringWithSpecialSymbols(_))
    tokens
  }
}
