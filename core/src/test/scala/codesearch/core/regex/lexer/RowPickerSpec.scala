package codesearch.core.regex.lexer

import org.scalatest.WordSpec
import codesearch.core.regex.lexer.tokens._
import codesearch.core.regex.lexer.RowPicker

class RowPickerSpec extends WordSpec {
  "Any Sequnce of Tokens" when {
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" should {
      """Tokens are collected into string -- "Hello World"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
        assert(strFromTokens == "Hello World")
      }
    }
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale), SpecialSymbol(' '), SpecialSymbol('/'), Content("Symbol")")""" should {
      """Tokens are collected into string -- "Hello World [^Gared] (Bale) /Symbol"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(
          Seq(
            Content("Hello"),
            SpecialSymbol(' '),
            Content("World"),
            SpecialSymbol(' '),
            Other("[^Gared]"),
            SpecialSymbol(' '),
            Other("(Bale)"),
            SpecialSymbol(' '),
            SpecialSymbol('/'),
            Content("Symbol")
          ))
        assert(strFromTokens == "Hello World [^Gared] (Bale) /Symbol")
      }
    }
  }
}
