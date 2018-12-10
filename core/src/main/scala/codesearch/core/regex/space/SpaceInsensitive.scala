package codesearch.core.regex.space

import codesearch.core.regex.lexer.{StringAssembler, Tokenizer}
import codesearch.core.regex.lexer.tokens._

object SpaceInsensitive {

  /**
    * Make a regex space-insensitive in places where one space is used.
    *
    * Only single spaces are affected, everything else (e.g. "  " or "\ ") is left as-is.
    *
    * @param query regex to transform. Example: "hello world   kek ?"
    * @return regex with added symbols "+" for space-insensitivity. Example: "hello +world   kek( +)?"
    */
  def spaceInsensitiveString(query: String): String = {
    val tokens: Seq[Token] = Tokenizer.parseStringWithSpecialSymbols(query)

    val allocatedOneOrMoreSpaces: List[Token] =
      List(SpecialSymbol("("), SpecialSymbol(" "), SpecialSymbol("+"), SpecialSymbol(")")).reverse

    val addedRegexForSpaceInsensitive: Seq[Token] = tokens
      .foldLeft(List.empty[Token]) { (result, current) =>
        result match {
          case SpecialSymbol(" ") :: SpecialSymbol(" ") :: _ =>
            current :: result
          case SpecialSymbol(" ") :: _ =>
            current match {
              case SpecialSymbol("+") => current :: result
              case SpecialSymbol("*") => current :: result
              case SpecialSymbol("?") => current :: (allocatedOneOrMoreSpaces ::: result.tail)
              case SpecialSymbol(" ") => current :: result
              case RepetitionSeq(_)   => current :: result
              case _                  => current :: SpecialSymbol("+") :: result
            }
          case _ =>
            current :: result
        }
      }
      .reverse

    StringAssembler.buildStringFromTokens(addedRegexForSpaceInsensitive)
  }
}
