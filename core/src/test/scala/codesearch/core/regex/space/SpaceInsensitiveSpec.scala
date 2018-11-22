package codesearch.core.regex.space

import org.scalatest.{FreeSpec, Matchers}

class SpaceInsensitiveSpec extends FreeSpec with Matchers {
  def addingSpaceInsensitive(before: String, after: String) = {
    before in {
      SpaceInsensitive.spaceInsensitiveString(before) shouldBe after
    }
  }

  "Space Insensitive tests" - {
    "Test" - {
      addingSpaceInsensitive("Hello world", "Hello +World")

      addingSpaceInsensitive("foo  bar", "foo bar")

      addingSpaceInsensitive("foo +bar", "foo +bar")

      addingSpaceInsensitive("foo ?bar", "foo( +)?bar")
    }
  }
}
