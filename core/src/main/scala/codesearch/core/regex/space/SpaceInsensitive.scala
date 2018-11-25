package codesearch.core.regex.space

import codesearch.core.regex.lexer.{StringAssembler, Tokenizer}
import codesearch.core.regex.lexer.tokens.{SpecialSymbol, Token}

object SpaceInsensitive {

  /**
    * Make a regex space-insensitive in places where one space is used.
    *
    * Only single spaces are affected, everything else (e.g. "  " or "\ ") is left as-is.
    *
    * @param query regex to transform. Example: "hello world   kek ?"
    * @return string with added symbols "+" for insensitive of spaces. Example: "hello +world   kek( +)?"
    */
  def spaceInsensitiveString(query: String): String = {
    val tokens: Seq[Token] = Tokenizer.parseStringWithSpecialSymbols(query)

    val selectMoreSpaces: List[Token] =
      List(SpecialSymbol(")"), SpecialSymbol("+"), SpecialSymbol(" "), SpecialSymbol("("))

    val addedRegexForSpaceInsensitive: Seq[Token] = tokens
      .foldLeft(List.empty[Token]) {
        case (result @ SpecialSymbol(" ") :: SpecialSymbol(" ") :: _, current) => current :: result
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol("+"))  => current :: result
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol("*"))  => current :: result
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol("{"))  => current :: result
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol("}"))  => current :: result
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol("?")) =>
          current :: (selectMoreSpaces ::: result.tail)
        case (result @ SpecialSymbol(" ") :: _, current @ SpecialSymbol(" ")) => current :: result
        case (result @ SpecialSymbol(" ") :: _, current)                      => current :: SpecialSymbol("+") :: result
        case (result, current)                                                => current :: result
      }
      .reverse

    StringAssembler.buildStringFromTokens(addedRegexForSpaceInsensitive)
  }
}
