package codesearch.core.meta

import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}

import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.config.JavaScriptConfig
import codesearch.core.index.repository.Downloader
import com.softwaremill.sttp._
import fs2.io.file
import fs2.text._
import fs2.{Pipe, Stream}
import fs2json._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe._
import io.circe.fs2._
import io.circe.generic.auto._
import io.circe.syntax._

class NpmMetaDownloader[F[_]: Sync: ContextShift](
    config: JavaScriptConfig,
    downloader: Downloader[F],
    logger: Logger[F]
) extends MetaDownloader[F] {

  def downloadMeta: F[Unit] =
    for {
      _ <- logger.info("Downloading javascript meta information")
      _ <- Sync[F].delay(config.repoJsonPath.getParent.toFile.mkdirs())
      _ <- downloader
        .download(Uri(config.repoIndexUrl))
        .through(tokenParser[F])
        .through(tokenFilter)
        .through(prettyPrinter())
        .through(cutStream)
        .through(byteArrayParser[F])
        .through(decoder[NpmRegistryPackage])
        .map(_.asJson.noSpaces + "\n")
        .through(utf8Encode)
        .through(file.writeAll(config.repoJsonPath, BlockingEC, List(CREATE, TRUNCATE_EXISTING)))
        .compile
        .drain
      _ <- logger.info("Downloading finished")
    } yield ()

  def tokenFilter: Pipe[F, JsonToken, JsonToken] =
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

  def cutStream: Pipe[F, Byte, Byte] = { input =>
    var depth = 0
    input.filter { byte =>
      if (byte == '[') {
        depth += 1; true
      } else if (byte == ']') {
        depth -= 1; true
      } else depth > 0
    }
  }

  def decoder[A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }
}

final case class NpmRegistryPackage(name: String, version: String)

object NpmRegistryPackage {
  implicit val docDecoder: Decoder[NpmRegistryPackage] = { c =>
    val doc = c.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield NpmRegistryPackage(name, tag)
  }
}

object NpmMetaDownloader {
  def apply[F[_]: Sync: ContextShift](config: JavaScriptConfig, downloader: Downloader[F]): F[MetaDownloader[F]] =
    for {
      logger <- Slf4jLogger.create
    } yield new NpmMetaDownloader(config, downloader, logger)
}
