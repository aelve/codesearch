package codesearch.core.index.regex.lexer.tokens

trait Token {
  def repr: String = this match {
    case Other(valueToken) => valueToken.toString
    case SpecialSymbol(valueToken) => valueToken.toString
    case Content(valueToken) => valueToken.toString
  }
}

final case class Other(value: String) extends Token

final case class Content(value: String) extends Token

final case class SpecialSymbol(value: Char) extends Token