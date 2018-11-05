package codesearch.core.index.regex.lexer

import org.scalatest.WordSpec
import codesearch.core.index.regex.lexer.tokens._
import codesearch.core.index.regex.lexer.Tokenizer
import fastparse._
import NoWhitespace._

class TokenizerSpec extends WordSpec {
  "String" when {
    """"Hello World"""" should {
      """Decompose into tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World")
        assert(tokens == Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
      }
    }

    """"Hello World + ?"""" should {
      """Decompose into tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), SpecialSymbol('+'),  SpecialSymbol(' '),  SpecialSymbol('?))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World + ?")
        assert(tokens == Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), SpecialSymbol('+'), SpecialSymbol(' '), SpecialSymbol('?')))
      }
    }

    """Hello World [^Gared]""" should {
      """Decompose into tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]")""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World [^Gared]")
        assert(tokens == Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]")))
      }
    }

    """Hello World [^Gared] (Bale)""" should {
      """Decompose into tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World [^Gared] (Bale)")
        assert(tokens == Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)")))
      }
    }
  }
}