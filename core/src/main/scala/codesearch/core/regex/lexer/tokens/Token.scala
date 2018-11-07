package codesearch.core.regex.lexer.tokens

sealed trait Token {
  def repr: String = this match {
    case Other(valueToken)         => valueToken
    case SpecialSymbol(valueToken) => valueToken.toString
    case Literal(valueToken)       => valueToken
    case Escaped(valueToken)       => s"\\${valueToken}"
  }
}

final case class Other(value: String) extends Token

final case class Literal(value: String) extends Token

final case class SpecialSymbol(value: Char) extends Token

final case class Escaped(value: Char) extends Token
