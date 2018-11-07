package codesearch.core.regex.lexer.tokens

sealed trait Token {
  def repr: String = this match {
    case Other(valueToken)         => valueToken.toString
    case SpecialSymbol(valueToken) => valueToken.toString
    case Literal(valueToken)       => valueToken.toString
    case Escaped(valueToken)       => s"\\${valueToken.toString}"
  }
}

final case class Other(value: String) extends Token

final case class Literal(value: String) extends Token

final case class SpecialSymbol(value: Char) extends Token

final case class Escaped(value: Char) extends Token
