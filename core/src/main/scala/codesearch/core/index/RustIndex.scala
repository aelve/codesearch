package codesearch.core.index

import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, RustConfig}
import codesearch.core.db.CratesDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СindexDirectory
import codesearch.core.index.repository.{CratesPackage, SourcesDownloader}
import codesearch.core.model.CratesTable
import codesearch.core.util.Helper
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import slick.jdbc.PostgresProfile.api._

final class RustIndex(
    rustConfig: RustConfig,
    val db: Database,
    val cindexDir: СindexDirectory
)(
    implicit
    val shift: ContextShift[IO],
    sourcesDownloader: SourcesDownloader[IO, CratesPackage]
) extends LanguageIndex[CratesTable] with CratesDB {

  private val IgnoreFiles = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    "archive.zip"
  )

  protected def concurrentTasksCount: Int = rustConfig.concurrentTasksCount

  protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(CratesPackage(name, version))
  }

  protected def getLastVersions: Stream[IO, (String, String)] = {
    implicit val packageDecoder: Decoder[(String, String)] = { c =>
      for {
        name    <- c.get[String]("name")
        version <- c.get[String]("vers")
      } yield name -> version
    }

    Helper
      .recursiveListFiles(rustConfig.repoPath.toFile)
      .filter(file => !IgnoreFiles.contains(file.getName))
      .evalMap(file => Helper.readFileAsync(file.getAbsolutePath).map(_.last))
      .through(stringStreamParser)
      .through(decoder[IO, (String, String)])
  }

  protected def buildFsUrl(packageName: String, version: String): Path =
    CratesPackage(packageName, version).packageDir
}

object RustIndex {
  def apply(
      config: Config,
      db: Database,
      cindexDir: СindexDirectory
  )(
      implicit
      shift: ContextShift[IO],
      sourcesDownloader: SourcesDownloader[IO, CratesPackage]
  ) = new RustIndex(config.languagesConfig.rust, db, cindexDir)
}
