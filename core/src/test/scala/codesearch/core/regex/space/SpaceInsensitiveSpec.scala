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
    }

    "Strings with regexp" - {
      addingSpaceInsensitive("foo +bar", "foo +bar")

      addingSpaceInsensitive("foo ?bar", "foo( +)?bar")

      addingSpaceInsensitive("foo *bar", "foo *bar")
    }

    "Strings with other cases" - {
      addingSpaceInsensitive("foo [bar]", "foo +[bar]")

      addingSpaceInsensitive("foo {bar}", "foo {bar}")

      addingSpaceInsensitive("foo [b ar]", "foo +[b ar]")
    }

    "Strings with other cases and regex" - {
      addingSpaceInsensitive("foo *[bar]", "foo *[bar]")

      addingSpaceInsensitive("foo +{bar}", "foo +{bar}")

      addingSpaceInsensitive("foo   [bar]", "foo   [bar]")
    }
  }
}
