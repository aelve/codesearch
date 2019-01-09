package codesearch.core.index.details
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.{Path, Paths}

import cats.Semigroup
import cats.effect.{ContextShift, IO}
import codesearch.core.index.details.GemDetails.FsIndexRoot
import codesearch.core.index.directory.Directory
import codesearch.core.index.directory.Preamble._
import codesearch.core.index.repository.FileDownloader
import codesearch.core.model.Version
import com.softwaremill.sttp.{SttpBackend, _}
import fs2.Stream
import io.circe.{Decoder, HCursor}
import org.apache.commons.io.FileUtils

import scala.sys.process._

private final case class GemPackage(name: String, version: String)

private[index] final class GemDetails(implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]], shift: ContextShift[IO]) {

  private val GemIndexUrl             = uri"http://rubygems.org/latest_specs.4.8.gz"
  private val FsRawIndexPath: Path    = FsIndexRoot / "latest_specs.4.8.gz"
  private val FsJsonIndexPath: Path   = FsIndexRoot / "gem_packages_index.json"
  private val RubyPath: Path          = Paths.get("/usr/bin/ruby")
  private val DeserializeScript: Path = Paths.get("./scripts/update_index.rb")

  private implicit val versionSemigroup: Semigroup[Version] = (_, last) => last

  private implicit val docDecoder: Decoder[GemPackage] = (c: HCursor) => {
    val doc = c.downField("doc")
    for {
      name <- doc.get[String]("name")
      distTag = doc.downField("dist-tags")
      tag <- distTag.get[String]("latest")
    } yield GemPackage(name, tag)
  }

  def index: IO[Unit] =
    for {
      _        <- IO(FileUtils.deleteDirectory(FsIndexRoot.toFile))
      rawIndex <- new FileDownloader().download(GemIndexUrl, FsRawIndexPath)
      _        <- toJson(rawIndex)
    } yield ()

  def details: IO[Map[String, Version]] = {

  }

  private def toJson(rawIndex: File): IO[Unit] = IO(
    Seq(RubyPath, DeserializeScript, FsRawIndexPath, FsJsonIndexPath).map(_.toFile.getAbsolutePath) !!
  )
}

private[index] object GemDetails {
  val FsIndexRoot: Path = Directory.metaInfoDir / "gem"
  def apply()(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new GemDetails()
}
