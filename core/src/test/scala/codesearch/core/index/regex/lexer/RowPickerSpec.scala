package codesearch.core.index.regex.lexer

import org.scalatest.WordSpec
import codesearch.core.index.regex.lexer.tokens._
import codesearch.core.index.regex.lexer.RowPicker

class RowPickerSpec extends WordSpec {
  "Any Sequnce of Tokens" when {
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" should {
      """Tokens are collected into string -- "Hello World"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
        assert(strFromTokens == "Hello World")
      }
    }
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")""" should {
      """Tokens are collected into string -- "Hello World [^Gared] (Bale)"""" in {
        val strFromTokens = RowPicker.buildStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")))
        assert(strFromTokens == "Hello World [^Gared] (Bale)")
      }
    }
  }
}
