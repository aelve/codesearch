package codesearch.core.meta.parser

import cats.effect.Sync
import codesearch.core.config.JavaScriptConfig
import codesearch.core.db.repository.PackageIndexTableRow
import fs2.{Pipe, Stream}
import fs2json.{JsonToken, TokenFilter, prettyPrinter, tokenParser}
import io.circe.fs2.byteArrayParser
import io.circe.{Decoder, Json}

final class JavaScriptIndexParser[F[_]: Sync](config: JavaScriptConfig) extends IndexByteStreamParser[F] {

  private implicit val docDecoder: Decoder[PackageIndexTableRow] = { cursor =>
    val doc = cursor.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield PackageIndexTableRow(name, tag, config.repository)
  }

  def parse(stream: Stream[F, Byte]): F[Stream[F, PackageIndexTableRow]] = {
    Sync[F].pure(
      stream
        .through(tokenParser[F])
        .through(tokenFilter)
        .through(prettyPrinter())
        .through(cutStream)
        .through(byteArrayParser[F])
        .through(decoder))
  }

  private def tokenFilter: Pipe[F, JsonToken, JsonToken] =
    TokenFilter.downObject
      .downField("rows")
      .downArray
      .downObject
      .downField("doc")
      .downObject
      .removeFields(
        Set(
          "_id",
          "_rev",
          "versions",
          "description",
          "maintainers",
          "homepage",
          "keywords",
          "readme",
          "author",
          "bugs",
          "license",
          "readmeFilename"
        )
      )

  private def cutStream: Pipe[F, Byte, Byte] = { input =>
    var depth = 0
    input.filter { byte =>
      if (byte == '[') {
        depth += 1; true
      } else if (byte == ']') {
        depth -= 1; true
      } else depth > 0
    }
  }

  private def decoder(implicit decode: Decoder[PackageIndexTableRow]): Pipe[F, Json, PackageIndexTableRow] = { input =>
    input.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }
  }
}

object JavaScriptIndexParser {
  def apply[F[_]: Sync](config: JavaScriptConfig): JavaScriptIndexParser[F] =
    new JavaScriptIndexParser(config)
}
