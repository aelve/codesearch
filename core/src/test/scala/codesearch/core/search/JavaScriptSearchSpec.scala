package codesearch.core.search

class JavaScriptSearchSpec extends SearchSpecBase {
  "Checking on the way to tests" - {
    javaScriptIsTestInWay(path = "path/to/package/tests/", result = true)
    javaScriptIsTestInWay(path = "path/to/package/sPEc-class/", result = true)
    javaScriptIsTestInWay(path = "path/to/package/tEsts.js", result = true)
    javaScriptIsTestInWay(path = "path/to/package/tes-t/", result = false)
  }
}
