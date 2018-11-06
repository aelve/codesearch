package codesearch.core.regex.lexer

import fastparse._
import NoWhitespace._
import codesearch.core.regex.lexer.tokens._

trait TokenizerMethods {
  def leftForOtherSymbols[_: P] = P(CharIn("(", "[", "[^")).!

  def rightForOtherSymbols[_: P] = P(CharIn(")", "]")).!

  def specialSymbols[_: P] = P(CharIn("/", " ", "$", "%", "^", "&", "*", "+", "?", "!")).!

  def parserAnyStringInOtherSymbols[_: P] = P(!rightForOtherSymbols ~ AnyChar).rep.!

  def parserOtherSymbols[_: P] =
    P(leftForOtherSymbols ~ parserAnyStringInOtherSymbols ~ rightForOtherSymbols).rep(1).!.map(Other(_))

  def parserSpecialSymbol[_: P] =
    P(specialSymbols.map(specialSymbolInString => SpecialSymbol(specialSymbolInString.charAt(0))))

  def parserAnyStringToSpecialSymbol[_: P] = P((!specialSymbols ~ AnyChar).rep(1).!.map(Content(_)))

  def parseStringWithSpecialSymbols[_: P] =
    P(parserOtherSymbols | parserAnyStringToSpecialSymbol | parserSpecialSymbol).rep
}

object Tokenizer extends TokenizerMethods {

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
