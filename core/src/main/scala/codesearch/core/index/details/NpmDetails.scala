package codesearch.core.index.details

import java.nio.ByteBuffer
import java.nio.file.Paths

import cats.effect.IO
import cats.syntax.either._
import codesearch.core.index.repository.DownloadException
import codesearch.core.model.Version
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.{Uri, _}
import fs2.{Chunk, Pipe, Sink, Stream, text}
import fs2.io._
import io.circe.fs2._
import io.circe.{Decoder, HCursor, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

private final case class Doc(name: String, tag: LatestTag)
private final case class NpmPackageDetails(name: String, version: String)
private final case class LatestTag(latest: String) extends AnyVal

private[index] final class NpmDetails(implicit ec: ExecutionContext, http: SttpBackend[IO, Stream[IO, ByteBuffer]]) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val FsIndexPath        = Paths.get("./index/npm_packages.json")
  private val NpmRegistryUrl     = uri"https://replicate.npmjs.com"
  private val NpmPackagesKeysUrl = uri"$NpmRegistryUrl/_all_docs?include_docs=true"

  private implicit val docDecoder: Decoder[Doc] = (c: HCursor) =>
    for {
      name <- c.get[String]("name")
      tag  <- c.get[LatestTag]("dist-tags")
    } yield Doc(name, tag)

  private implicit val npmPackageDetailsDecoder: Decoder[NpmPackageDetails] =
    (c: HCursor) => c.get[Doc]("doc").map(doc => NpmPackageDetails(doc.name, doc.tag.latest))


  def detailsMap = {
    file.readAllAsync[IO](FsIndexPath, chunkSize = 4096)
        .through(byteArrayParser[IO])
        .through(decoder[IO, NpmPackageDetails])
        .through(toMap)
        .compile
        .toList

  }

  private def toMap[F[_]]: Pipe[IO, NpmPackageDetails, Map[String, Version]] = { input =>
    input.flatMap(details => Stream(Map(details.name -> Version(details.version))))
  }

  def index: IO[Unit] =
    stream(NpmPackagesKeysUrl)
      .flatMap(
        _.through(toBytes)
          .through(cutStream)
          .through(byteArrayParser[IO])
          .through(decoder[IO, NpmPackageDetails])
          .through(packageToString)
          .to(toFile)
          .compile
          .drain)

  private def stream(url: Uri): IO[Stream[IO, ByteBuffer]] = {
    sttp
      .get(url)
      .response(asStream[Stream[IO, ByteBuffer]])
      .readTimeout(Duration.Inf)
      .send
      .flatMap(response => IO.fromEither(response.body.leftMap(DownloadException)))
  }

  private def toBytes[F[_]]: Pipe[IO, ByteBuffer, Byte] = { input =>
    input.flatMap(buffer => Stream.chunk(Chunk.array(buffer.array)))
  }

  private def cutStream: Pipe[IO, Byte, Byte] = { input =>
    var depth = 0
    input.filter { byte =>
      if (byte == '[') {
        depth += 1; true
      } else if (byte == ']') {
        depth -= 1; true
      } else depth > 0
    }
  }

  private def decoder[F[_], A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }

  private def packageToString[F[_]]: Pipe[IO, NpmPackageDetails, Byte] = { input =>
    input.flatMap(details => Stream.chunk(Chunk.array(details.asJson.noSpaces.getBytes)))
  }

  private def toFile: Sink[IO, Byte] = file.writeAllAsync(FsIndexPath)

}

object NpmDetails {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]]
  ) = new NpmDetails()
}
