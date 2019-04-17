package codesearch.core.search

class JavaScriptSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    javaScriptIsTestInWay("path/to/package/tests/", true)
    javaScriptIsTestInWay("path/to/package/sPEc-class/", true)
    javaScriptIsTestInWay("path/to/package/tEsts.js", true)
    javaScriptIsTestInWay("path/to/package/tes-t/", false)
  }
}
