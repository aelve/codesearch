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

      addingSpaceInsensitive("foo bar ", "foo +bar +")

      addingSpaceInsensitive(" bar foo", " +bar +foo")

      addingSpaceInsensitive("  Test String  ", "  Test +String  ")

      addingSpaceInsensitive(" Gared Bale ", " +Gared +Bale +")

      addingSpaceInsensitive("Hello    world", "Hello    world")
    }

    "String with escaping" - {
      addingSpaceInsensitive("FOO\\ BAR", "FOO\\ BAR")

      addingSpaceInsensitive("Foo\\ Bar \\ ", "Foo\\ Bar +\\ ")

      addingSpaceInsensitive("Gared\\ Bale\\ ", "Gared\\ Bale\\ ")

      addingSpaceInsensitive("hello\\ world\\  ", "hello\\ world\\  +")

      addingSpaceInsensitive("test\\\\ str\\ ", "test\\\\ +str\\ ")
    }

    "Strings with regexp" - {
      addingSpaceInsensitive("foo +bar", "foo +bar")

      addingSpaceInsensitive("foo ?bar", "foo( +)?bar")

      addingSpaceInsensitive("foo *bar", "foo *bar")
    }

    "Strings with other cases" - {
      addingSpaceInsensitive("foo [bar]", "foo +[bar]")

      addingSpaceInsensitive("foo {1,3}", "foo {1,3}")

      addingSpaceInsensitive("foo {1,}", "foo {1,}")

      addingSpaceInsensitive("foo {1}", "foo {1}")

      addingSpaceInsensitive("foo {bar}", "foo +{bar}")

      addingSpaceInsensitive("foo [b ar]", "foo +[b ar]")
    }

    "Strings with other cases and regex" - {
      addingSpaceInsensitive("foo *[bar]", "foo *[bar]")

      addingSpaceInsensitive("foo +{bar}", "foo +{bar}")

      addingSpaceInsensitive("foo   {1,3}", "foo   {1,3}")

      addingSpaceInsensitive("foo   {1,}", "foo   {1,}")

      addingSpaceInsensitive("foo   {1}", "foo   {1}")

      addingSpaceInsensitive("foo   {bar}", "foo   {bar}")

      addingSpaceInsensitive("foo   [bar]", "foo   [bar]")
    }
  }
}
