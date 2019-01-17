package codesearch.core.index

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import codesearch.core.config.{Config, RustConfig}
import codesearch.core.db.CratesDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions._
import codesearch.core.index.repository.{CratesPackage, FileDownloader}
import codesearch.core.model.CratesTable
import codesearch.core.util.Helper
import com.softwaremill.sttp.{SttpBackend, _}
import fs2.Stream
import org.apache.commons.io.FileUtils
import org.rauschig.jarchivelib.ArchiveFormat.ZIP
import org.rauschig.jarchivelib.ArchiverFactory
import play.api.libs.json.Json
import codesearch.core.index.directory.Preamble._

class RustIndex(rustConfig: RustConfig)(
    implicit val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO]
) extends LanguageIndex[CratesTable] with CratesDB {

  private val GithubUrl = uri"https://github.com/rust-lang/crates.io-index/archive/master.zip"
  private val RepoDir   = new File("./data/meta/rust")
  private val IgnoreFiles = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    "archive.zip"
  )

  override protected type Tag = Rust

  override protected val csearchDir: СSearchDirectory[Tag] = implicitly

  override protected def concurrentTasksCount: Int = rustConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    archiveDownloadAndExtract(CratesPackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] = {
    for {
      _       <- IO(FileUtils.deleteDirectory(RepoDir))
      archive <- new FileDownloader().download(GithubUrl, RepoDir.toPath / "archive.zip")
      _       <- IO(ArchiverFactory.createArchiver(ZIP).extract(archive, RepoDir))
    } yield ()
  }

  override protected def getLastVersions: Stream[IO, (String, String)] =
    Helper
      .recursiveListFiles(RepoDir)
      .collect {
        case file if !(IgnoreFiles contains file.getName) =>
          val lastVersionJSON = scala.io.Source.fromFile(file).getLines().toSeq.last
          val obj             = Json.parse(lastVersionJSON)
          val name            = (obj \ "name").as[String]
          val vers            = (obj \ "vers").as[String]
          (name -> vers)
      }

  override protected def buildFsUrl(packageName: String, version: String): Path =
    CratesPackage(packageName, version).packageDir
}

object RustIndex {
  def apply(config: Config)(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new RustIndex(config.languagesConfig.rustConfig)
}
