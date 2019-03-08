package codesearch.core.regex.space

import codesearch.core.regex.lexer.{StringAssembler, Tokenizer}
import codesearch.core.regex.lexer.tokens._

import scala.annotation.tailrec

object SpaceInsensitiveString {

  /**
    * Make a regex space-insensitive in places where one space is used.
    *
    * Only single spaces are affected, everything else (e.g. "  " or "\ ") is left as-is.
    *
    * @param query regex to transform. Example: "hello world   kek ?"
    * @return regex with added symbols "+" for space-insensitivity. Example: "hello +world   kek( +)?"
    */
  def apply(query: String): String = {
    val tokens: List[Token] = Tokenizer.parseStringWithSpecialSymbols(query).toList
    val allocatedOneOrMoreSpaces: List[Token] =
      List(SpecialSymbol("("), Space(" "), SpecialSymbol("+"), SpecialSymbol(")")).reverse

    @tailrec
    def loop(result: List[Token], remaining: List[Token]): List[Token] = (result, remaining) match {
      case (_, Nil)                                     => result
      case (Space(_) :: Space(_) :: _, current :: next) => loop(current :: result, next)
      case (Space(_) :: _, current :: next) =>
        current match {
          case Space(_)           => loop(current :: result, next)
          case SpecialSymbol("+") => loop(current :: result, next)
          case SpecialSymbol("?") => loop(current :: (allocatedOneOrMoreSpaces ::: result.tail), next)
          case SpecialSymbol("*") => loop(current :: result, next)
          case RepetitionSeq(_)   => loop(current :: result, next)
          case _                  => loop(current :: SpecialSymbol("+") :: result, next)
        }
      case (_, current :: Nil) =>
        current match {
          case Space(_)           => SpecialSymbol("+") :: current :: result
          case SpecialSymbol("+") => current :: result
          case _                  => current :: result
        }
      case (_, current :: next) => loop(current :: result, next)
    }

    val addedRegexForSpaceInsensitive: Seq[Token] = loop(List.empty[Token], tokens).reverse

    StringAssembler(addedRegexForSpaceInsensitive)
  }
}
