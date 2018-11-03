package codesearch.core.lexer.tokens

trait Token {
  def repr: String = this match {
    case Other(valueToken) => valueToken.toString
    case SpecialSymbol(valueToken) => valueToken.toString
    case Content(valueToken) => valueToken.toString
  }
}
