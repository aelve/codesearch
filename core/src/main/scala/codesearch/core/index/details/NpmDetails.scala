package codesearch.core.index.details

import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}

import cats.effect.{ContextShift, IO}
import cats.instances.map._
import codesearch.core._
import codesearch.core.index.details.NpmDetails.FsIndexRoot
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Preamble._
import codesearch.core.index.repository.ByteStreamDownloader
import com.softwaremill.sttp.{SttpBackend, _}
import fs2.io._
import fs2.{Chunk, Pipe, Sink, Stream}
import fs2json._
import io.circe.fs2._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, HCursor, Json}

import scala.language.higherKinds

private final case class NpmRegistryPackage(name: String, version: String)
private final case class NpmPackage(name: String, version: String)

private[index] final class NpmDetails(implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]], shift: ContextShift[IO]) {

  private val NpmRegistryUrl = uri"https://replicate.npmjs.com/_all_docs?include_docs=true"
  private val FsIndexPath    = FsIndexRoot / "npm_packages_index.json"

  private implicit val docDecoder: Decoder[NpmRegistryPackage] = (c: HCursor) => {
    val doc = c.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield NpmRegistryPackage(name, tag)
  }

  private val excludeFields =
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

  def index: IO[Unit] =
    new ByteStreamDownloader()
      .download(NpmRegistryUrl)
      .flatMap(
        _.through(tokenParser[IO])
          .through(tokenFilter)
          .through(prettyPrinter())
          .through(cutStream)
          .through(byteArrayParser[IO])
          .through(decoder[IO, NpmRegistryPackage])
          .through(packageToString)
          .to(toFile)
          .compile
          .drain)

  def detailsMap: Stream[IO, (String, String)] = {
    file
      .readAll[IO](FsIndexPath, BlockingEC, chunkSize = 4096)
      .through(byteStreamParser[IO])
      .through(decoder[IO, NpmPackage])
      .through(toCouple)
  }

  private def tokenFilter[F[_]]: Pipe[F, JsonToken, JsonToken] =
    TokenFilter.downObject
      .downField("rows")
      .downArray
      .downObject
      .downField("doc")
      .downObject
      .removeFields(excludeFields)

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

  private def toCouple[F[_]]: Pipe[IO, NpmPackage, (String, String)] =
    _.map(npmPackage => (npmPackage.name -> npmPackage.version))

  private def decoder[F[_], A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }

  private def packageToString[F[_]]: Pipe[IO, NpmRegistryPackage, Byte] =
    _.flatMap(registryPackage => Stream.chunk(Chunk.array(registryPackage.asJson.noSpaces.getBytes :+ '\n'.toByte)))

  private def toFile: Sink[IO, Byte] = file.writeAll(FsIndexPath, BlockingEC, List(CREATE, TRUNCATE_EXISTING))

}

private[index] object NpmDetails {
  val FsIndexRoot: Path = Directory.metaInfoDir / "npm"
  def apply()(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new NpmDetails()
}
