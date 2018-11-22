package codesearch.core.regex.lexer

import org.scalatest.{FreeSpec, Matchers}
import codesearch.core.regex.lexer.tokens._

class TokenizerSpec extends FreeSpec with Matchers {

  def testParseAndRender(value: String, tokens: Seq[Token]): Unit = {
    value in {
      Tokenizer.parseStringWithSpecialSymbols(value) shouldBe tokens
      StringAssembler.buildStringFromTokens(tokens) shouldBe value
    }
  }

  "Roundtrip tests" - {

    "Big examples" - {
      testParseAndRender("Hello World", Seq(Literal("Hello"), SpecialSymbol(" "), Literal("World")))

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
          SpecialSymbol("("),
          Literal("Bale"),
          SpecialSymbol(")"),
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
          SpecialSymbol("("),
          Literal("Bale"),
          SpecialSymbol(")"),
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

    "Empty lexer" - {
      testParseAndRender("", Seq())
    }

    "Literals" - {
      testParseAndRender("a", Seq(Literal("a")))
      testParseAndRender("123,ab-c", Seq(Literal("123,ab-c")))
    }

    // TODO: all of these should really be just literals.
    "Curly braces as literals" - {
      testParseAndRender("{1,2", Seq(SpecialSymbol("{"), Literal("1,2")))
      testParseAndRender("1,2}", Seq(Literal("1,2"), SpecialSymbol("}")))
      testParseAndRender("{1,2,}", Seq(SpecialSymbol("{"), Literal("1,2,"), SpecialSymbol("}")))
      testParseAndRender("{a,2}", Seq(SpecialSymbol("{"), Literal("a,2"), SpecialSymbol("}")))
      testParseAndRender("{-1,2}", Seq(SpecialSymbol("{"), Literal("-1,2"), SpecialSymbol("}")))
    }

    "Special symbols" - {
      testParseAndRender(".", Seq(SpecialSymbol(".")))
      testParseAndRender("|", Seq(SpecialSymbol("|")))
      testParseAndRender("^", Seq(SpecialSymbol("^")))
      testParseAndRender("$", Seq(SpecialSymbol("$")))
      testParseAndRender("ax.,.c",
                         Seq(Literal("ax"), SpecialSymbol("."), Literal(","), SpecialSymbol("."), Literal("c")))
      testParseAndRender("a|^", Seq(Literal("a"), SpecialSymbol("|"), SpecialSymbol("^")))
      testParseAndRender("a|b", Seq(Literal("a"), SpecialSymbol("|"), Literal("b")))
      testParseAndRender("(a)|b",
                         Seq(SpecialSymbol("("), Literal("a"), SpecialSymbol(")"), SpecialSymbol("|"), Literal("b")))
      testParseAndRender("a*", Seq(Literal("a"), SpecialSymbol("*")))
      testParseAndRender("a??", Seq(Literal("a"), SpecialSymbol("?"), SpecialSymbol("?")))
    }

    "Spaces" - {
      testParseAndRender(" ", Seq(SpecialSymbol(" ")))
      testParseAndRender("a  b", Seq(Literal("a"), SpecialSymbol(" "), SpecialSymbol(" "), Literal("b")))
    }

    // TODO: these examples give somewhat incorrect results. Properly they
    // should be parsed as Other or as their own case.
    "Repetition" - {
      testParseAndRender("a{2}", Seq(Literal("a"), SpecialSymbol("{"), Literal("2"), SpecialSymbol("}")))
      testParseAndRender("a{2,3}", Seq(Literal("a"), SpecialSymbol("{"), Literal("2,3"), SpecialSymbol("}")))
      testParseAndRender("a{2,}", Seq(Literal("a"), SpecialSymbol("{"), Literal("2,"), SpecialSymbol("}")))
      testParseAndRender("a{2}?",
                         Seq(Literal("a"), SpecialSymbol("{"), Literal("2"), SpecialSymbol("}"), SpecialSymbol("?")))
    }

    "Escaping" - {
      testParseAndRender("\\|", Seq(Escaped('|')))
      testParseAndRender("\\\\a", Seq(Escaped('\\'), Literal("a")))
      testParseAndRender("\\d", Seq(Escaped('d')))
      // TODO: incorrect
      testParseAndRender("\\123", Seq(Escaped('1'), Literal("23")))
      testParseAndRender("\\p{Greek}", Seq(Escaped('p'), SpecialSymbol("{"), Literal("Greek"), SpecialSymbol("}")))
    }

    "Character sets" - {
      testParseAndRender("[^a-z]", Seq(Other("[^a-z]")))
      // Parsed as set('[')
      testParseAndRender("[[]", Seq(Other("[[]")))
      // Parsed as set(']')
      testParseAndRender("[]]", Seq(Other("[]]")))
      // Parsed as set('[a]')
      testParseAndRender("[a]]", Seq(Other("[a]"), SpecialSymbol("]")))
      // Parsed as set('[', ']')
      testParseAndRender("[][]", Seq(Other("[][]")))
      // Parsed as two sets
      testParseAndRender("[][] [][]", Seq(Other("[][]"), SpecialSymbol(" "), Other("[][]")))
      // Parsed as two sets
      testParseAndRender("[[] []]", Seq(Other("[[]"), SpecialSymbol(" "), Other("[]]")))
      testParseAndRender("[\\]\\\\]", Seq(SpecialSymbol("["), Escaped(']'), Escaped('\\'), SpecialSymbol("]")))
      testParseAndRender("[{3,5}]", Seq(Other("[{3,5}]")))
      testParseAndRender("[{3,5]}", Seq(Other("[{3,5]"), SpecialSymbol("}")))
    }

  }

}
