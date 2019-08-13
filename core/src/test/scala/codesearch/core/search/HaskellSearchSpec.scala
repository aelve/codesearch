package codesearch.core.search

class HaskellSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    haskellIsTestInWay(path = "path/to/package/test/", result = true)
    haskellIsTestInWay(path = "path/to/package/testSUites/", result = true)
    haskellIsTestInWay(path = "path/to/package/tEsts/", result = true)
    haskellIsTestInWay(path = "path/to/package/tes-t/", result = false)
  }
}
