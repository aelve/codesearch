package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.Path

import ammonite.ops.pwd
import cats.effect.IO
import codesearch.core.db.CratesDB
import codesearch.core.index.repository.CratesPackage
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.model
import codesearch.core.model.{CratesTable, Version}
import codesearch.core.util.Helper
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

class RustIndex(
    private val ec: ExecutionContext,
    private val httpClient: SttpBackend[IO, Stream[IO, ByteBuffer]]
) extends LanguageIndex[CratesTable] with CratesDB {

  override protected implicit def executor: ExecutionContext                    = ec
  override protected implicit def http: SttpBackend[IO, Stream[IO, ByteBuffer]] = httpClient

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".crates_csearch_index"
  override protected val langExts: String  = ".*\\.(rs)$"

  private val REPO_DIR = pwd / 'data / 'rust / "crates.io-index"
  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

  override def downloadMetaInformation(): Unit = {
    s"git -C $REPO_DIR pull" !!
  }

  override protected def updateSources(name: String, version: String): Future[Int] = {
    archiveDownloadAndExtract(CratesPackage(name, version))
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

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"

  override protected def buildFsUrl(packageName: String, version: String): Path =
    CratesPackage(packageName, version).packageDir
}

object RustIndex {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]]
  ) = new RustIndex(ec, http)
}
