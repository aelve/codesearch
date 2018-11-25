package codesearch.core.regex.space

import org.scalatest.{FreeSpec, Matchers}

class SpaceInsensitiveSpec extends FreeSpec with Matchers {
  def addingSpaceInsensitive(before: String, after: String) = {
    before in {
      SpaceInsensitive.spaceInsensitiveString(before) shouldBe after
    }
  }

  "Space Insensitive tests" - {
    "Simple strings" - {
      addingSpaceInsensitive("Hello world", "Hello +world")

      addingSpaceInsensitive("Hello    world", "Hello    world")

      addingSpaceInsensitive("foo\\ bar", "foo\\ bar")
    }

    "Strings with regexp" - {
      addingSpaceInsensitive("foo +bar", "foo +bar")

      addingSpaceInsensitive("foo ?bar", "foo( +)?bar")

      addingSpaceInsensitive("foo *bar", "foo *bar")
    }

    "Strings with other cases" - {
      addingSpaceInsensitive("foo [bar]", "foo +[bar]")

      addingSpaceInsensitive("foo {1,3}", "foo {1,3}")

      addingSpaceInsensitive("foo [b ar]", "foo +[b ar]")
    }

    "*Not valid tests*" - {

      /**
        * A vot i net, this should actually become foo +{bar} because not everything in curly braces is parsed as a range.
        */
      addingSpaceInsensitive("foo {bar}", "foo {bar}")
    }

    "Strings with other cases and regex" - {
      addingSpaceInsensitive("foo *[bar]", "foo *[bar]")

      addingSpaceInsensitive("foo +{bar}", "foo +{bar}")

      addingSpaceInsensitive("foo   [bar]", "foo   [bar]")
    }
  }
}
