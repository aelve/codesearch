package codesearch.core.regex.lexer.tokens

sealed trait Token {
  def repr: String = this match {
    case CharSet(valueToken)       => valueToken
    case SpecialSymbol(valueToken) => valueToken
    case Literal(valueToken)       => """([\[\]{}()^$\\|*+?.])""".r.replaceAllIn(valueToken, """\\$1""")
    case Escaped(valueToken)       => s"\\$valueToken"
    case RepetitionSeq(valueToken) => valueToken
    case Space(valueToken)         => valueToken
  }
}

final case class CharSet(value: String) extends Token

final case class Literal(value: String) extends Token

final case class SpecialSymbol(value: String) extends Token

final case class Escaped(value: String) extends Token

final case class RepetitionSeq(value: String) extends Token

final case class Space(value: String) extends Token
