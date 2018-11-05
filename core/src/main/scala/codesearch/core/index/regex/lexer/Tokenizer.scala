package codesearch.core.index.regex.lexer

import fastparse._
import NoWhitespace._
import codesearch.core.index.regex.lexer.tokens._

trait TokenizerMethods {
  protected def leftForOtherSymbols[_: P] = P(CharIn("(", "[", "[^")).!

  protected def rightForOtherSymbols[_: P] = P(CharIn(")", "]")).!

  protected def specialSymbols[_: P] = P(CharIn(" ", "$", "%", "^", "&", "*", "+", "?", "!")).!

  protected def parserAnyStringInOtherSymbols[_: P] = P(!rightForOtherSymbols ~ AnyChar).rep.!

  protected def parserOtherSymbols[_: P] =
    P(leftForOtherSymbols ~ parserAnyStringInOtherSymbols ~ rightForOtherSymbols).rep(1).!.map(Other(_))

  protected def parserSpecialSymbol[_: P] =
    P(specialSymbols.map(specialSymbolInString => SpecialSymbol(specialSymbolInString.charAt(0))))

  protected def parserAnyStringToSpecialSymbol[_: P] = P((!specialSymbols ~ AnyChar).rep(1).!.map(Content(_)))

  protected def parseStringWithSpecialSymbols[_: P] =
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
