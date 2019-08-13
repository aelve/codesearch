package codesearch.core.search

import java.nio.file.Paths

import codesearch.core.index.directory.{HaskellCindex, JavaScriptCindex, RubyCindex, RustCindex}
import org.scalatest.{FreeSpec, Matchers}


trait SearchSpecBase extends FreeSpec with Matchers{
  val haskellCindex = HaskellCindex(Paths.get("./index/test/cindex/"))
  val haskellSearch = new HaskellSearch(haskellCindex)
  val rubyCindex = RubyCindex(Paths.get("./index/test/cindex/"))
  val rubySearch = new RubySearch(rubyCindex)
  val rustCindex = RustCindex(Paths.get("./index/test/cindex/"))
  val rustSearch = new RustSearch(rustCindex)
  val javaScriptCindex = JavaScriptCindex(Paths.get("./index/test/cindex/"))
  val javaScriptSearch = new JavaScriptSearch(javaScriptCindex)

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
