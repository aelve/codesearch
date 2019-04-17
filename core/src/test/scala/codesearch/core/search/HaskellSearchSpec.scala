package codesearch.core.search

class HaskellSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    haskellIsTestInWay("path/to/package/test/", true)
    haskellIsTestInWay("path/to/package/testSUites/", true)
    haskellIsTestInWay("path/to/package/tEsts/", true)
    haskellIsTestInWay("path/to/package/tes-t/", false)
  }
}
