package codesearch.core.regex.lexer

import org.scalatest.{Matchers, WordSpec}
import codesearch.core.regex.lexer.tokens._
import codesearch.core.regex.lexer.Tokenizer

class TokenizerSpec extends WordSpec with Matchers{
  "String" when {
    """"Hello World"""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World")
        tokens shouldBe Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"))
      }
    }

    """"Hello World + ?"""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), SpecialSymbol(' '), SpecialSymbol('+'),  SpecialSymbol(' '),  SpecialSymbol('?))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World + ?")
        tokens shouldBe Seq(Literal("Hello"),
                      SpecialSymbol(' '),
                      Literal("World"),
                      SpecialSymbol(' '),
                      SpecialSymbol('+'),
                      SpecialSymbol(' '),
                      SpecialSymbol('?'))
      }
    }

    """Hello World [^Gared]""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), SpecialSymbol(' '), Other("[^Gared]")""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World [^Gared]")
        tokens shouldBe Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), SpecialSymbol(' '), Other("[^Gared]"))
      }
    }

    """Hello World(Kek) [^Gared] (Bale) \Symbol""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), Other("Kek"), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)", SpecialSymbol(' '), Escaped('S'), Literal("ymbol"))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World [^Gared] (Bale) \\Symbol")
        tokens shouldBe Seq(
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
        )
      }
    }

    """Hello World\(Kek\) [^Gared] (Bale) \Symbol \Kek+""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(' '), Literal("World"), Escaped('('), Literal("Kek"), Escaped(')'), SpecialSymbol(' '), Other("[^Gared]"), SpecialSymbol(' '), Other("(Bale)", SpecialSymbol(' '), Escaped('S'), Literal("ymbol"), SpecialSymbol(' '), Escaped('K'), Literal("ek"), SpecialSymbol('+'))""" in {
        val tokens = Tokenizer.parseStringWithSpecialSymbols("Hello World\\(Kek\\) [^Gared] (Bale) \\Symbol \\Kek+")
        tokens shouldBe Seq(
          Literal("Hello"),
          SpecialSymbol(' '),
          Literal("World"),
          Escaped('('),
          Literal("Kek"),
          Escaped(')'),
          SpecialSymbol(' '),
          Other("[^Gared]"),
          SpecialSymbol(' '),
          Other("(Bale)"),
          SpecialSymbol(' '),
          Escaped('S'),
          Literal("ymbol"),
          SpecialSymbol(' '),
          Escaped('K'),
          Literal("ek"),
          SpecialSymbol('+')
        )
      }
    }
  }
}
