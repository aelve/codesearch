package codesearch.core.search

class RubySearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    rubyIsTestInWay("path/to/package/test/", true)
    rubyIsTestInWay("path/to/package/spec/", true)
    rubyIsTestInWay("path/to/package/tEsts/", false)
    rubyIsTestInWay("path/to/package/tes-t/", false)
  }
}
