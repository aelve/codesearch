package codesearch.core.meta.unarchiver

import cats.effect.Sync
import codesearch.core.config.LanguageConfig
import codesearch.core.db.repository.PackageIndex
import fs2.{Pipe, Stream}
import fs2json.{JsonToken, TokenFilter, prettyPrinter, tokenParser}
import io.circe.fs2.byteArrayParser
import io.circe.{Decoder, Json}

final class JavaScriptUnarchiver[F[_]: Sync](
    config: LanguageConfig
) extends StreamIndexUnarchiver[F] {

  private implicit val docDecoder: Decoder[PackageIndex] = { cursor =>
    val doc = cursor.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield PackageIndex(name, tag, config.repository)
  }

  def packages: Pipe[F, Byte, PackageIndex] = { input =>
    input
      .through(tokenParser[F])
      .through(tokenFilter)
      .through(prettyPrinter())
      .through(cutStream)
      .through(byteArrayParser[F])
      .through(decoder)
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

  private def decoder(implicit decode: Decoder[PackageIndex]): Pipe[F, Json, PackageIndex] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }
}

object JavaScriptUnarchiver {
  def apply[F[_]: Sync](
      config: LanguageConfig
  ): JavaScriptUnarchiver[F] = new JavaScriptUnarchiver(config)
}
