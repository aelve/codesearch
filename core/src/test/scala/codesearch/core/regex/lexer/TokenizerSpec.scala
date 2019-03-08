package codesearch.core.regex.lexer

import scala.io.Source
import org.scalatest.{FreeSpec, Matchers}
import codesearch.core.regex.lexer.tokens._

class TokenizerSpec extends FreeSpec with Matchers {

  def testParseAndRender(value: String, tokens: Seq[Token]): Unit = {
    value in {
      Tokenizer.parseStringWithSpecialSymbols(value) shouldBe tokens
      StringAssembler(tokens) shouldBe value
    }
  }

  /** Break a regex into tokens and then reassemble it from the tokens. */
  def roundTrip(caseString: String): String = {
    StringAssembler(Tokenizer.parseStringWithSpecialSymbols(caseString))
  }

  "Roundtrip tests" - {

    "Big examples" - {
      testParseAndRender("Hello World", Seq(Literal("Hello"), Space(" "), Literal("World")))

      testParseAndRender(
        "Hello World + ?",
        Seq(Literal("Hello"),
            Space(" "),
            Literal("World"),
            Space(" "),
            SpecialSymbol("+"),
            Space(" "),
            SpecialSymbol("?"))
      )

      testParseAndRender("Hello World [^Gared]",
                         Seq(Literal("Hello"), Space(" "), Literal("World"), Space(" "), CharSet("[^Gared]")))

      testParseAndRender(
        "Hello World [^Gared] (Bale) \\Symbol",
        Seq(
          Literal("Hello"),
          Space(" "),
          Literal("World"),
          Space(" "),
          CharSet("[^Gared]"),
          Space(" "),
          SpecialSymbol("("),
          Literal("Bale"),
          SpecialSymbol(")"),
          Space(" "),
          Escaped("S"),
          Literal("ymbol")
        )
      )

      testParseAndRender(
        "\\(Kek\\) [^Gared] (Bale) \\Symbol \\Kek+",
        Seq(
          Escaped("("),
          Literal("Kek"),
          Escaped(")"),
          Space(" "),
          CharSet("[^Gared]"),
          Space(" "),
          SpecialSymbol("("),
          Literal("Bale"),
          SpecialSymbol(")"),
          Space(" "),
          Escaped("S"),
          Literal("ymbol"),
          Space(" "),
          Escaped("K"),
          Literal("ek"),
          SpecialSymbol("+")
        )
      )
    }

    "Empty regex" - {
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
      testParseAndRender(" ", Seq(Space(" ")))
      testParseAndRender("a  b", Seq(Literal("a"), Space(" "), Space(" "), Literal("b")))
    }

    "Repetition" - {
      testParseAndRender("a{2}", Seq(Literal("a"), RepetitionSeq("{2}")))
      testParseAndRender("a{2,3}", Seq(Literal("a"), RepetitionSeq("{2,3}")))
      testParseAndRender(
        "a{2, 3}",
        Seq(Literal("a"), SpecialSymbol("{"), Literal("2,"), Space(" "), Literal("3"), SpecialSymbol("}")))
      testParseAndRender("a{,3}", Seq(Literal("a"), SpecialSymbol("{"), Literal(",3"), SpecialSymbol("}")))
      testParseAndRender("a{2,}", Seq(Literal("a"), RepetitionSeq("{2,}")))
      testParseAndRender("a{,}", Seq(Literal("a"), SpecialSymbol("{"), Literal(","), SpecialSymbol("}")))
      testParseAndRender("a{2}?", Seq(Literal("a"), RepetitionSeq("{2}"), SpecialSymbol("?")))
    }

    "Escaping" - {
      testParseAndRender("\\|", Seq(Escaped("|")))
      testParseAndRender("\\\\a", Seq(Escaped("\\"), Literal("a")))
      testParseAndRender("\\d", Seq(Escaped("d")))
      //TODO: Incorrect
      testParseAndRender("\\123", Seq(Escaped("1"), Literal("23")))
      testParseAndRender("\\p{Greek}", Seq(Escaped("p"), SpecialSymbol("{"), Literal("Greek"), SpecialSymbol("}")))
    }

    "Character sets" - {
      testParseAndRender("[^a-z]", Seq(CharSet("[^a-z]")))
      // Parsed as set("[")
      testParseAndRender("[[]", Seq(CharSet("[[]")))
      // Parsed as set("]")
      testParseAndRender("[]]", Seq(CharSet("[]]")))
      // Parsed as set("[a]")
      testParseAndRender("[a]]", Seq(CharSet("[a]"), SpecialSymbol("]")))
      // Parsed as set("[", "]")
      testParseAndRender("[][]", Seq(CharSet("[][]")))
      // Parsed as two sets
      testParseAndRender("[][] [][]", Seq(CharSet("[][]"), Space(" "), CharSet("[][]")))
      // Parsed as two sets
      testParseAndRender("[[] []]", Seq(CharSet("[[]"), Space(" "), CharSet("[]]")))
      testParseAndRender("[\\]\\\\]", Seq(CharSet("[\\]\\\\]")))
      testParseAndRender("[{3,5}]", Seq(CharSet("[{3,5}]")))
      testParseAndRender("[{3,5]}", Seq(CharSet("[{3,5]"), SpecialSymbol("}")))
      testParseAndRender("[\\foo]", Seq(CharSet("[\\foo]")))
      testParseAndRender("[a-z\\n\\t]", Seq(CharSet("[a-z\\n\\t]")))
      testParseAndRender("[a-z [:alpha:] foo [:bar:]]", Seq(CharSet("[a-z [:alpha:] foo [:bar:]]")))
      testParseAndRender("[[:alpha:]]", Seq(CharSet("[[:alpha:]]")))
    }

    "POSIX classes test" - {
      testParseAndRender("foo [:alpha:]", Seq(Literal("foo"), Space(" "), CharSet("[:alpha:]")))
    }

    "RoundTrip cases" - {
      val cases = Source.fromResource("regex/cases.txt").getLines
      cases.foreach { caseString => caseString shouldBe roundTrip(caseString)
      }
    }

  }

}
