package codesearch.core.regex.lexer

import org.scalatest.{Matchers, WordSpec}
import codesearch.core.regex.lexer.tokens._

class TokenizerSpec extends WordSpec with Matchers {

  def testParseAndRender(value: String, tokens: Seq[Token]):Unit = {
    Tokenizer.parseStringWithSpecialSymbols(value) shouldBe tokens
    StringAssembler.buildStringFromTokens(tokens) shouldBe value
  }

  "String" when {
    """"Hello World"""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World"))""" in {
        testParseAndRender("Hello World", Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World")))
      }
    }

    """"Hello World + ?"""" should {
      """Decompose into tokens -- Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World"), SpecialSymbol(" "), SpecialSymbol("+"),  SpecialSymbol(" "),  SpecialSymbol("?"))""" in {
        testParseAndRender(
          "Hello World + ?",
          Seq(Literal("Hello"),
              SpecialSymbol(" "),
              Literal("World"),
              SpecialSymbol(" "),
              SpecialSymbol("+"),
              SpecialSymbol(" "),
              SpecialSymbol("?"))
        )
      }
    }

    """"strings""" should {
      """be parsed as a whole""" in {
        testParseAndRender(
          "Hello World [^Gared]",
          Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World"), SpecialSymbol(" "), Other("[^Gared]")))

        testParseAndRender(
          "Hello World [^Gared] (Bale) \\Symbol",
          Seq(
            Literal("Hello"),
            SpecialSymbol(" "),
            Literal("World"),
            SpecialSymbol(" "),
            Other("[^Gared]"),
            SpecialSymbol(" "),
            Other("(Bale)"),
            SpecialSymbol(" "),
            Escaped('S'),
            Literal("ymbol")
          )
        )

        testParseAndRender(
          "\\(Kek\\) [^Gared] (Bale) \\Symbol \\Kek+",
          Seq(
            Escaped('('),
            Literal("Kek"),
            Escaped(')'),
            SpecialSymbol(" "),
            Other("[^Gared]"),
            SpecialSymbol(" "),
            Other("(Bale)"),
            SpecialSymbol(" "),
            Escaped('S'),
            Literal("ymbol"),
            SpecialSymbol(" "),
            Escaped('K'),
            Literal("ek"),
            SpecialSymbol("+")
          )
        )
      }
    }

    """Correctly parses escaped bracket in character set""" in {
      testParseAndRender("[\\]]", Seq(SpecialSymbol("["), Escaped(']'), SpecialSymbol("]")))
    }
  }
}
