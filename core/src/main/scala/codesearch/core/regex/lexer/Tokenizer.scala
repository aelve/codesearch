package codesearch.core.regex.lexer

import fastparse._
import NoWhitespace._
import codesearch.core.regex.lexer.tokens._

object Tokenizer {

  private def leftForCharSet[_: P] = P("[]" | "[" | "[^").!

  private def rightForCharSet[_: P] = P("]").!

  private def specialSymbols[_: P] =
    P("\\" | " " | "." | "|" | "$" | "%" | "^" | "&" | "*" | "+" | "?" | "!" | "[" | "]" | "{" | "}" | "(" | ")").!

  private def parserEscaped[_: P] = P("\\" ~ AnyChar.!).map(a => Escaped(a.charAt(0)))

  private def parserCharInsideSet[_: P] = P(!"\\" ~ !rightForCharSet ~ AnyChar).rep.!

  private def parserCharSet[_: P] =
    P(leftForCharSet ~ parserCharInsideSet ~ rightForCharSet).rep(1).!.map(Other(_))

  private def parserSpecialSymbol[_: P] =
    P(specialSymbols.map(specialSymbolInString => SpecialSymbol(specialSymbolInString)))

  private def parserAnyStringBeforeSpecialSymbol[_: P] = P((!specialSymbols ~ AnyChar).rep(1).!.map(Literal(_)))

  private def parseStringWithSpecialSymbols[_: P] =
    P(parserEscaped | parserCharSet | parserAnyStringBeforeSpecialSymbol | parserSpecialSymbol).rep

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
