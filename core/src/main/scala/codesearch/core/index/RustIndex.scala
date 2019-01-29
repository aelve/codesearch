package codesearch.core.index

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, RustConfig}
import codesearch.core.db.CratesDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.syntax.path._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.RustCSearchIndex
import codesearch.core.index.repository.{CratesPackage, Downloader, SourcesDownloader}
import codesearch.core.model.CratesTable
import codesearch.core.util.Helper
import com.softwaremill.sttp.{SttpBackend, _}
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import org.apache.commons.io.FileUtils
import org.rauschig.jarchivelib.ArchiveFormat.ZIP
import org.rauschig.jarchivelib.ArchiverFactory

class RustIndex(rustConfig: RustConfig)(
    implicit val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO],
    downloader: Downloader[IO],
    sourcesDownloader: SourcesDownloader[IO, CratesPackage]
) extends LanguageIndex[CratesTable] with CratesDB {

  private val GithubUrl = uri"https://github.com/rust-lang/crates.io-index/archive/master.zip"
  private val RepoDir   = new File("./data/meta/rust")
  private val IgnoreFiles = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    "archive.zip"
  )

  override protected val csearchDir: СSearchDirectory = RustCSearchIndex

  override protected def concurrentTasksCount: Int = rustConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(CratesPackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] = {
    for {
      _       <- IO(FileUtils.deleteDirectory(RepoDir))
      archive <- downloader.download(GithubUrl, RepoDir.toPath / "archive.zip")
      _       <- IO(ArchiverFactory.createArchiver(ZIP).extract(archive, RepoDir))
    } yield ()
  }

  override protected def getLastVersions: Stream[IO, (String, String)] = {
    implicit val packageDecoder: Decoder[(String, String)] = { c =>
      for {
        name    <- c.get[String]("name")
        version <- c.get[String]("vers")
      } yield name -> version
    }

    Helper
      .recursiveListFiles(RepoDir)
      .filter(file => !IgnoreFiles.contains(file.getName))
      .evalMap(file => Helper.readFileAsync(file.getAbsolutePath).map(_.last))
      .through(stringStreamParser)
      .through(decoder[IO, (String, String)])
  }

  override protected def buildFsUrl(packageName: String, version: String): Path =
    CratesPackage(packageName, version).packageDir
}

object RustIndex {
  def apply(config: Config)(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO],
      downloader: Downloader[IO],
      sourcesDownloader: SourcesDownloader[IO, CratesPackage]
  ) = new RustIndex(config.languagesConfig.rustConfig)
}
