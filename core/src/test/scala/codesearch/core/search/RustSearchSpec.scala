package codesearch.core.search

class RustSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    rustIsTestInWay(path = "path/to/package/tests/", result = true)
    rustIsTestInWay(path = "path/to/package/spec/", result = false)
    rustIsTestInWay(path = "path/to/package/tEsts/", result = false)
    rustIsTestInWay(path = "path/to/package/tes-t/", result = false)
  }
}
