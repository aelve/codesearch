package codesearch.core.index

import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, JavaScriptConfig}
import codesearch.core.db.NpmDB
import codesearch.core.index.details.NpmDetails
import codesearch.core.index.repository.{NpmPackage, SourcesDownloader}
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СindexDirectory
import codesearch.core.model.NpmTable
import fs2.Stream
import slick.jdbc.PostgresProfile.api._

final class JavaScriptIndex(
    config: JavaScriptConfig,
    val db: Database,
    val cindexDir: СindexDirectory
)(
    implicit
    val shift: ContextShift[IO],
    sourcesDownloader: SourcesDownloader[IO, NpmPackage]
) extends LanguageIndex[NpmTable] with NpmDB {

  protected def concurrentTasksCount: Int = config.concurrentTasksCount

  protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(NpmPackage(name, version))
  }

  protected def getLastVersions: Stream[IO, (String, String)] = NpmDetails(config).detailsMap

  protected def buildFsUrl(packageName: String, version: String): Path =
    NpmPackage(packageName, version).packageDir
}

object JavaScriptIndex {
  def apply(
      config: Config,
      db: Database,
      cindexDir: СindexDirectory
  )(
      implicit
      shift: ContextShift[IO],
      sourcesDownloader: SourcesDownloader[IO, NpmPackage]
  ) = new JavaScriptIndex(config.languagesConfig.javascript, db, cindexDir)
}
