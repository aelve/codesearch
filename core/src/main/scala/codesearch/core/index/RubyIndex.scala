package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.{Path, Paths}

import ammonite.ops.pwd
import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core._
import codesearch.core.config.{Config, RubyConfig}
import codesearch.core.db.GemDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions._
import codesearch.core.index.repository.GemPackage
import codesearch.core.model.GemTable
import com.softwaremill.sttp.SttpBackend
import io.circe.fs2._
import fs2.{Pipe, Stream}
import fs2.io.file
import io.circe.{Decoder, Json}

import scala.sys.process._

class RubyIndex(rubyConfig: RubyConfig)(
    implicit val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO]
) extends LanguageIndex[GemTable] with GemDB {

  private val GEM_INDEX_URL     = "http://rubygems.org/latest_specs.4.8.gz"
  private val GEM_INDEX_ARCHIVE = pwd / 'data / 'meta / 'ruby / "ruby_index.gz"
  private val GEM_INDEX_JSON    = Paths.get((pwd / 'data / 'meta / 'ruby / "ruby_index.json").toString)
  private val DESERIALIZER_PATH = pwd / 'scripts / "update_index.rb"

  override protected type Tag = Ruby

  override protected val csearchDir: СSearchDirectory[Tag] = implicitly

  override protected def concurrentTasksCount: Int = rubyConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(GemPackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] = IO {
    (pwd / 'data / 'meta / 'ruby).toIO.mkdirs()
    Seq("curl", "-o", GEM_INDEX_ARCHIVE.toString, GEM_INDEX_URL) !!

    Seq("/usr/bin/ruby", DESERIALIZER_PATH.toString, GEM_INDEX_ARCHIVE.toString, GEM_INDEX_JSON.toString()) !!
  }

  override protected def getLastVersions: Stream[IO, (String, String)] = {
    file
      .readAll[IO](GEM_INDEX_JSON, BlockingEC, 4096)
      .through(byteStreamParser[IO])
      .through(decoder[IO, Seq[String]])
      .through(toCouple)
  }

  private def toCouple[F[_]]: Pipe[IO, Seq[String], (String, String)] = { input =>
    input.map { case Seq(name :: ver :: _) => (name -> ver) }
  }

  private def decoder[F[_], A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(_)  => Stream.empty
        case Right(a) => Stream.emit(a)
      }
    }

  override protected def buildFsUrl(packageName: String, version: String): Path =
    GemPackage(packageName, version).packageDir
}

object RubyIndex {
  def apply(config: Config)(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new RubyIndex(config.languagesConfig.rubyConfig)
}
