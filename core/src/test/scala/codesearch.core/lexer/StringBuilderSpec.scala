package codesearch.core.lexer

import org.scalatest.WordSpec
import codesearch.core.lexer.tokens._
import codesearch.core.lexer.StringBuilder

class StringBuilderSpec extends WordSpec {
  "Any Sequnce a Tokens" when {
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" should {
      """Tokens collect a string -- "Hello World"""" in {
        val strFromTokens = StringBuilder.buildStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
        assert(strFromTokens == "Hello World")
      }
    }
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")""" should {
      """Tokens collect a string -- "Hello World [^Gared] (Bale)"""" in {
        val strFromTokens = StringBuilder.buildStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")))
        assert(strFromTokens == "Hello World [^Gared] (Bale)")
      }
    }
  }
}
