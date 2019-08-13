package codesearch.core.search

class RubySearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    rubyIsTestInWay(path = "path/to/package/test/", result = true)
    rubyIsTestInWay(path = "path/to/package/spec/", result = true)
    rubyIsTestInWay(path = "path/to/package/tEsts/", result = false)
    rubyIsTestInWay(path = "path/to/package/tes-t/", result = false)
  }
}
