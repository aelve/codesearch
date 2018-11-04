package codesearch.core.lexer

import org.scalatest.WordSpec
import codesearch.core.lexer.tokens._
import codesearch.core.lexer.Tokenizer
import fastparse._
import NoWhitespace._

class TokenizerSpec extends WordSpec {
  "String" when {
    """"Hello World"""" should {
      """Decompose in tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"))""" in {
        val Parsed.Success(value, _) = parse("Hello World", Tokenizer.parseStringWithSpecialSymbols(_))
        assert(value == Seq(Content("Hello"), SpecialSymbol(' '), Content("World")))
      }
    }

    """"Hello World + ?"""" should {
      """Decompose in tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), SpecialSymbol('+'),  SpecialSymbol(' '),  SpecialSymbol('?))""" in {
        val Parsed.Success(value, _) = parse("Hello World + ?", Tokenizer.parseStringWithSpecialSymbols(_))
        assert(value == Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), SpecialSymbol('+'),  SpecialSymbol(' '),  SpecialSymbol('?')))
      }
    }

    """Hello World \\P{Gare}""" should {
      """Decompose in tokens -- Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), Other("\\P{Gared}")""" in {
        val Parsed.Success(value, _) = parse("Hello World + ?", Tokenizer.parseStringWithSpecialSymbols(_))
        assert(value == Seq(Content("Hello"), SpecialSymbol(' '), Content("World"), SpecialSymbol(' '), SpecialSymbol('+'),  SpecialSymbol(' '),  SpecialSymbol('?')))
      }
    }
  }
}