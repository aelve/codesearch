package codesearch.core.regex.lexer.tokens

sealed trait Token {
  def repr: String = this match {
    case CharSet(valueToken)       => valueToken
    case SpecialSymbol(valueToken) => valueToken.toString
    case Literal(valueToken)       => valueToken
    case Escaped(valueToken)       => s"\\${valueToken}"
  }
}

final case class CharSet(value: String) extends Token

final case class Literal(value: String) extends Token

final case class SpecialSymbol(value: String) extends Token

final case class Escaped(value: Char) extends Token
