package codesearch.core.regex.lexer

import org.scalatest.WordSpec
import codesearch.core.regex.lexer.tokens._
import codesearch.core.regex.lexer.RowPicker

class RowPickerSpec extends WordSpec {
  "Any Sequnce of Tokens" when {
    """Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"))""" should {
      """Tokens are collected into string -- "Hello World"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World")))
        assert(strFromTokens == "Hello World")
      }
    }
    """Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale), SpecialSymbol(' '), Escaped('S'), Literal("ymbol")")""" should {
      """Tokens are collected into string -- "Hello World [^Gared] (Bale) \Symbol"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(
          Seq(
            Literal("Hello"),
            SpecialSymbol(' '),
            Literal("World"),
            SpecialSymbol(' '),
            Other("[^Gared]"),
            SpecialSymbol(' '),
            Other("(Bale)"),
            SpecialSymbol(' '),
            Escaped('S'),
            Literal("ymbol")
          ))
        assert(strFromTokens == "Hello World [^Gared] (Bale) \\Symbol")
      }
    }

    """Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), Escaped('('), Literal("Kek"), Escaped(')'))""" should {
      """Tokens are collected into string -- "Hello World \(Kek\)"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(
          Seq(Literal("Hello"),
              SpecialSymbol(' '),
              Literal("World"),
              SpecialSymbol(' '),
              Escaped('('),
              Literal("Kek"),
              Escaped(')')))
        assert(strFromTokens == "Hello World \\(Kek\\)")
      }
    }
  }
}
