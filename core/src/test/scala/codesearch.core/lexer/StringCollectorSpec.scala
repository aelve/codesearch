package codesearch.core.lexer

import org.scalatest.WordSpec
import codesearch.core.lexer.tokens._
import codesearch.core.lexer.StringsCollector

class StringCollectorSpec extends WordSpec{
  "Any Sequnce a Tokens" when {
    """Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" should {
      """Tokens collect a string -- "Hello World"""" in {
        val strFromTokens = StringsCollector.collectStringFromTokens(Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
        assert(strFromTokens == "Hello World")
      }
    }
  }
}
