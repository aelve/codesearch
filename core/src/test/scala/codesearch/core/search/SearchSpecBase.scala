package codesearch.core.search

import org.scalatest.{FreeSpec, Matchers}


trait SearchSpecBase extends FreeSpec with Matchers{
  val haskellSearch = new HaskellSearch
  val rubySearch = new RubySearch
  val rustSearch = new RustSearch
  val javaScriptSearch = new JavaScriptSearch

  def haskellIsTestInWay(path: String, result: Boolean): Unit = {
    path in {
      haskellSearch.isTestInPath(path) shouldBe result
    }
  }

  def rubyIsTestInWay(path: String, result: Boolean): Unit = {
    path in {
      rubySearch.isTestInPath(path) shouldBe result
    }
  }

  def rustIsTestInWay(path: String, result: Boolean): Unit = {
    path in {
      rustSearch.isTestInPath(path) shouldBe result
    }
  }

  def javaScriptIsTestInWay(path: String, result: Boolean): Unit = {
    path in {
      javaScriptSearch.isTestInPath(path) shouldBe result
    }
  }
}
