package codesearch.core.search

import cats.data.NonEmptyVector
import codesearch.core.config.SnippetConfig
import codesearch.core.search.SnippetsGrouper.SnippetInfo
import org.scalatest.{Matchers, WordSpec}

class SnippetsGrouperSpec extends WordSpec with Matchers {

  "Groups consecutive snippets from the same file" in {

    val matchedLines = Seq(
      "3models/0.3.0/Graphics/Model/DirectX.hs:13:import Control.Applicative",
      "3models/0.3.0/Graphics/Model/DirectX.hs:14:import Data.Attoparsec.ByteString.Char8 as A",
      "3models/0.3.0/Graphics/Model/DirectX.hs:15:import qualified Data.ByteString as B",
      "3models/0.3.0/Graphics/Model/DirectX.hs:16:import Data.Traversable",
      "3models/0.3.0/Graphics/Model/DirectX.hs:17:import Data.Word",
    )

    val snippets: List[SnippetInfo] = fs2.Stream
      .emits(matchedLines)
      .through(
        SnippetsGrouper.groupLines(SnippetConfig(pageSize = 30, linesBefore = 5, linesAfter = 5))
      )
      .compile
      .toList

    snippets should contain theSameElementsAs List(
      SnippetInfo(
        filePath = "3models/0.3.0/Graphics/Model/DirectX.hs",
        lines = NonEmptyVector.of(13, 14, 15, 16, 17)
      )
    )
  }

  "Doesn't group snippets if distance between them is too much" in {

    val matchedLines = Seq(
      "3models/0.3.0/Graphics/Model/DirectX.hs:28:} deriving Show",
      "3models/0.3.0/Graphics/Model/DirectX.hs:39:} deriving Show",
    )

    val snippets: List[SnippetInfo] = fs2.Stream
      .emits(matchedLines)
      .through(
        SnippetsGrouper.groupLines(SnippetConfig(pageSize = 30, linesBefore = 5, linesAfter = 5))
      )
      .compile
      .toList

    snippets should contain theSameElementsAs List(
      SnippetInfo(
        filePath = "3models/0.3.0/Graphics/Model/DirectX.hs",
        lines = NonEmptyVector.of(28)
      ),
      SnippetInfo(
        filePath = "3models/0.3.0/Graphics/Model/DirectX.hs",
        lines = NonEmptyVector.of(39)
      )
    )
  }

  "Doesn't group snippets from different files" in {

    val matchedLines = Seq(
      "3models/0.3.0/Graphics/Model/DirectX.hs:14:import Data.Attoparsec.ByteString.Char8 as A",
      "3models/0.3.0/Graphics/Model/Obj.hs:16:import Data.Attoparsec.ByteString.Char8 as A",
    )

    val snippets: List[SnippetInfo] = fs2.Stream
      .emits(matchedLines)
      .through(
        SnippetsGrouper.groupLines(SnippetConfig(pageSize = 30, linesBefore = 5, linesAfter = 5))
      )
      .compile
      .toList

    snippets should contain theSameElementsAs List(
      SnippetInfo(
        filePath = "3models/0.3.0/Graphics/Model/DirectX.hs",
        lines = NonEmptyVector.of(14)
      ),
      SnippetInfo(
        filePath = "3models/0.3.0/Graphics/Model/Obj.hs",
        lines = NonEmptyVector.of(16)
      )
    )
  }

}
