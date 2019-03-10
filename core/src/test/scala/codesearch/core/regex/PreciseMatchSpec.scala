package codesearch.core.regex

import org.scalatest.{FreeSpec, Matchers}

class PreciseMatchSpec extends FreeSpec with Matchers {
  def preciseStringMatch(before: String, after: String): Unit = {
    before in {
      PreciseMatch(before) shouldBe after
    }
  }
  def preciseStringWithCaseInsensitive(before: String, after: String): Unit = {
    before in {
      RegexConstructor(before, insensitive = true, space = false, precise = true).toString shouldBe after
    }
  }

  "Precise Match test" - {
    "Precise string with special symbols match" - {
      preciseStringMatch("Hello +world", """Hello \+world""")
      preciseStringMatch("Hello +world ?", """Hello \+world \?""")
      preciseStringMatch(" .?*+", """ \.\?\*\+""")
    }

    "Precise string with escaped" - {
      preciseStringMatch("""\""", """\\""")
      preciseStringMatch("""\\""", """\\\\""")
      preciseStringMatch("""\\\""", """\\\\\\""")
      preciseStringMatch("""\\o\n""", """\\\\o\\n""")
    }

    "Precise string with charsets match" - {
      preciseStringMatch("""Hello \ [hello]""", """Hello \\ \[hello\]""")
      preciseStringMatch("Test {1,2}", """Test \{1,2\}""")
      preciseStringMatch("Test {,2}", """Test \{,2\}""")
    }

    "Precise string with POSIX classes match" - {
      preciseStringMatch("Hello [[:alpha:]]", """Hello \[\[:alpha:\]\]""")
    }

    "Precise string with Unicode punctuation" - {
      // Should not be escaped because RE2 doesn't consider it "escapable" punctuation
      preciseStringMatch("†․‼", "†․‼")
    }

    "Precise string with case insensitive" - {
      preciseStringWithCaseInsensitive("""Hello \n\o""", """(?i)Hello \\n\\o""")
    }
  }
}
