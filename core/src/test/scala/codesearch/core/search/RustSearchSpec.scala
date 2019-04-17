package codesearch.core.search

class RustSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    rustIsTestInWay("path/to/package/test/", true)
    rustIsTestInWay("path/to/package/spec/", false)
    rustIsTestInWay("path/to/package/tEsts/", false)
    rustIsTestInWay("path/to/package/tes-t/", false)
  }
}
