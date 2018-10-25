package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.Path

import ammonite.ops.pwd
import cats.effect.{ContextShift, IO}
import codesearch.core.config.{Config, RustConfig}
import codesearch.core.db.CratesDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.CratesPackage
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions._
import codesearch.core.model
import codesearch.core.model.{CratesTable, Version}
import codesearch.core.util.Helper
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.sys.process._

class RustIndex(rustConfig: RustConfig)(
    implicit val executor: ExecutionContext,
    val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO]
) extends LanguageIndex[CratesTable] with CratesDB {

  private val REPO_DIR = pwd / 'data / 'rust / "crates.io-index"
  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

  override protected type Tag = Rust

  override protected val csearchDir: СSearchDirectory[Tag] = implicitly

  override protected def concurrentTasksCount: Int = rustConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    archiveDownloadAndExtract(CratesPackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] = IO {
    // See https://stackoverflow.com/a/41081908. Note that 'git init' and
    // 'git remote add origin' are idempotent and it's safe to run them
    // every time we want to download new packages.
    s"git -C $REPO_DIR init" !!;
    s"git -C $REPO_DIR remote add origin https://github.com/rust-lang/crates.io-index" !!;
    s"git -C $REPO_DIR fetch --depth 1" !!;
    s"git -C $REPO_DIR reset --hard origin/master" !!
  }

  override protected def getLastVersions: Map[String, Version] = {
    val seq = Helper
      .recursiveListFiles(REPO_DIR.toIO)
      .collect {
        case file if !(IGNORE_FILES contains file.getName) =>
          val lastVersionJSON = scala.io.Source.fromFile(file).getLines().toSeq.last
          val obj             = Json.parse(lastVersionJSON)
          val name            = (obj \ "name").as[String]
          val vers            = (obj \ "vers").as[String]
          (name, model.Version(vers))
      }
      .toSeq
    Map(seq: _*)
  }

  override protected def buildFsUrl(packageName: String, version: String): Path =
    CratesPackage(packageName, version).packageDir
}

object RustIndex {
  def apply(config: Config)(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new RustIndex(config.languagesConfig.rustConfig)
}
