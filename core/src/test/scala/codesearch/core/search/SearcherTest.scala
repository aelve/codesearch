package codesearch.core.search

import io.circe.parser._
import org.scalatest.{FlatSpec, Matchers}

class SearcherTest extends FlatSpec with Matchers {

  it should "decode zoekt result" in {
    val raw = "{\"FileName\":\"index.html\",\"Repository\":\"site\",\"LineNumber\":22,\"Line\":\"\\u003ch4\\u003e\\u003ca href=\\\"mailto:join@8bitcat.org\\\"\\u003ejoin@8bitcat.org\\u003c/a\\u003e\\u003c/h4\\u003e\",\"LineFragments\":[{\"LineOffset\":20,\"Offset\":824,\"MatchLength\":16},{\"LineOffset\":38,\"Offset\":842,\"MatchLength\":16}]}"
    val result = parse(raw).flatMap(Search.zoektResultDecoder.decodeJson)
    print(result)
    result.isRight shouldBe true
  }
}
