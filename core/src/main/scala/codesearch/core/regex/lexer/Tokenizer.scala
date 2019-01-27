package codesearch.core.regex.lexer

import fastparse._
import NoWhitespace._
import codesearch.core.regex.lexer.tokens._

object Tokenizer {

  /**
    * Parses string into a Tokens
    *
    * @param query search query
    * @return sequence tokens
    */
  def parseStringWithSpecialSymbols(query: String): Seq[Token] = {
    val Parsed.Success(tokens, _) = parse(query, parseStringWithSpecialSymbols(_))
    tokens
  }

  private def startForCharSet[_: P] = P("[]" | "[" | "[^").!

  private def endForCharSet[_: P] = P("]").!

  /** A POSIX character class, e.g. `[:alpha:]`. */
  private def charSetPred[_: P] = P("[:" ~ (!":]" ~ AnyChar).rep ~ ":]")

  /**Handles cases {n} and {n,} and {n, n}, example {33,}*/
  private def parseRepetitionSeq[_: P] =
    P("{" ~ (CharIn("0-9").rep(1) ~ ("," ~ CharIn("0-9").rep | "")) ~ "}").!.map(RepetitionSeq)

  private def specialSymbols[_: P] =
    P("\\" | "." | "|" | "$" | "%" | "^" | "&" | "*" | "+" | "?" | "!" | "[" | "]" | "{" | "}" | "(" | ")").!

  private def parserEscaped[_: P] = P("\\" ~ AnyChar.!).map(Escaped)

  private def parseSpaces[_: P] = P(" ").!.map(Space)

  private def parserCharInsideSet[_: P] = P(("\\" | !endForCharSet) ~ (charSetPred | AnyChar)).rep.!

  private def parserCharSet[_: P] =
    P(startForCharSet ~ parserCharInsideSet ~ endForCharSet).rep(1).!.map(CharSet)

  private def parserSpecialSymbol[_: P] =
    P(specialSymbols.map(specialSymbolInString => SpecialSymbol(specialSymbolInString)))

  private def parserAnyStringBeforeSpecialSymbol[_: P] = P((!" " ~ !specialSymbols ~ AnyChar).rep(1).!.map(Literal))

  private def parseStringWithSpecialSymbols[_: P] =
    P(parserEscaped | parserCharSet | parserAnyStringBeforeSpecialSymbol | parseSpaces | parseRepetitionSeq | parserSpecialSymbol).rep
}