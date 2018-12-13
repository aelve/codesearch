package codesearch.core.util

import org.scalatest.{FreeSpec, Matchers}
import codesearch.core.regex.lexer.tokens._

class HelperSpec extends FreeSpec with Matchers {
  def preciseStringMatch(before: String, after: String) = {
    before in {
      Helper.preciseMatch(before) shouldBe after
    }
  }

  "Precise Match test" - {
    "Precise string with special symbols match" - {
      preciseStringMatch("Hello +world", "Hello\\ \\+world")
      preciseStringMatch("Hello +world ?", "Hello\\ \\+world\\ \\?")
      preciseStringMatch(" .?*+", "\\ \\.\\?\\*\\+")
    }

    "Precise string with charsets match" - {
      preciseStringMatch("Hello [hello]", "Hello\\ \\[hello]")
      preciseStringMatch("Test {1,2}", "Test\\ \\{1,2}")
      preciseStringMatch("Test {,2}", "Test\\ \\{,2\\}")
    }

    "Precise string with POSIX classes match" - {
      preciseStringMatch("Hello [[:alpha:]]", "Hello\\ \\[[:alpha:]]")
    }
  }
}
