package codesearch.core.index.details

import java.nio.ByteBuffer
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.{Path, Paths}

import cats.Semigroup
import cats.effect.{ContextShift, IO}
import codesearch.core._
import codesearch.core.index.repository.ByteStreamDownloader
import codesearch.core.model.Version
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp._
import fs2.{Chunk, Pipe, Sink, Stream}
import fs2.io._
import io.circe.fs2._
import io.circe.{Decoder, HCursor, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import cats.instances.map._

import scala.language.higherKinds
import codesearch.core.index.directory.PathOps._
import codesearch.core.index.details.NpmDetails.FsIndexRoot

import scala.collection.mutable

private final case class NpmRegistryPackage(name: String, version: String)
private final case class NpmPackage(name: String, version: String)

private[index] final class NpmDetails(implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]], shift: ContextShift[IO]) {

  private val NpmRegistryUrl = uri"https://replicate.npmjs.com/_all_docs?include_docs=true"
  private val FsIndexPath    = FsIndexRoot / "npm_packages_index.json"

  private implicit val versionSemigroup: Semigroup[Version] = (_, last) => last

  private implicit val docDecoder: Decoder[NpmRegistryPackage] = (c: HCursor) => {
    val doc = c.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield NpmRegistryPackage(name, tag)
  }

  def index: IO[Unit] =
    new ByteStreamDownloader()
      .download(NpmRegistryUrl)
      .flatMap(
        _.through(cutStream)
          .through(byteArrayParser[IO])
          .through(decoder[IO, NpmRegistryPackage])
          .through(packageToString)
          .to(toFile)
          .compile
          .drain)

  def detailsMap: IO[Map[String, Version]] = {
    file
      .readAll[IO](FsIndexPath, BlockingEC, chunkSize = 4096)
      .through(byteStreamParser[IO])
      .through(decoder[IO, NpmPackage])
      .through(toMap)
      .compile
      .foldMonoid
  }

  private def toMap[F[_]]: Pipe[IO, NpmPackage, Map[String, Version]] = { input =>
    input.map(npmPackage => Map(npmPackage.name -> Version(npmPackage.version)))
  }

  private def cutStream: Pipe[IO, Byte, Byte] = { input =>
    var isOpen: Boolean = false
    val pattern         = Array[Byte]('}', '}', ']')
    val queue           = mutable.Queue[Byte]()
    input.filter { byte =>
      if (!isOpen) {
        if (byte == '[') { isOpen = true; true } else false
      } else if (queue.size == 3) {
        if (queue.containsSlice(pattern) && byte == '}') false
        else {
          queue.dequeue
          queue.enqueue(byte)
          true
        }
      } else { queue.enqueue(byte); true }
    }
  }

  private def decoder[F[_], A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }

  private def packageToString[F[_]]: Pipe[IO, NpmRegistryPackage, Byte] = { input =>
    input.flatMap(registryPackage => Stream.chunk(Chunk.array(registryPackage.asJson.noSpaces.getBytes :+ '\n'.toByte)))
  }

  private def toFile: Sink[IO, Byte] = file.writeAll(FsIndexPath, BlockingEC, List(CREATE, TRUNCATE_EXISTING))

}

private[index] object NpmDetails {
  val FsIndexRoot: Path = Paths.get("./index/npm")
  def apply()(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new NpmDetails()
}
