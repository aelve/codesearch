package codesearch.core.index.details

import cats.effect.{ContextShift, IO}
import codesearch.core._
import codesearch.core.config.JavaScriptConfig
import fs2.Stream
import fs2.io._
import io.circe.fs2._
import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}

import scala.language.higherKinds

private final case class NpmRegistryPackage(name: String, version: String)
private final case class NpmPackage(name: String, version: String)

private[index] final class NpmDetails(config: JavaScriptConfig)(implicit shift: ContextShift[IO]) {

  private implicit val docDecoder: Decoder[NpmRegistryPackage] = (c: HCursor) => {
    val doc = c.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield NpmRegistryPackage(name, tag)
  }

  def detailsMap: Stream[IO, (String, String)] = {
    file
      .readAll[IO](config.repoJsonPath, BlockingEC, chunkSize = 4096)
      .through(byteStreamParser[IO])
      .through(decoder[IO, NpmPackage])
      .map(npmPackage => npmPackage.name -> npmPackage.version)
  }
}

private[index] object NpmDetails {
  def apply(config: JavaScriptConfig)(implicit shift: ContextShift[IO]) = new NpmDetails(config)
}
