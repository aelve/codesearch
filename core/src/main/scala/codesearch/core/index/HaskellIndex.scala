package codesearch.core.index

import java.nio.file.{Path => NioPath}

import cats.Order
import cats.effect.{ContextShift, IO}
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import codesearch.core.config.{Config, HaskellConfig}
import codesearch.core.db.HackageDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory._
import codesearch.core.index.repository.{HackagePackage, SourcesDownloader}
import codesearch.core.model.{HackageTable, Version}
import fs2.{Chunk, Stream}
import slick.jdbc.PostgresProfile.api._

final class HaskellIndex(
    haskellConfig: HaskellConfig,
    val db: Database,
    val cindexDir: СindexDirectory
)(
    implicit
    val shift: ContextShift[IO],
    sourcesDownloader: SourcesDownloader[IO, HackagePackage]
) extends LanguageIndex[HackageTable] with HackageDB {

  protected def concurrentTasksCount: Int = haskellConfig.concurrentTasksCount

  protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(HackagePackage(name, version))
  }

  protected def getLastVersions: Stream[IO, (String, String)] = {
    Stream
      .evalUnChunk(IO(Chunk.array(haskellConfig.repoPath.toFile.listFiles)))
      .filter(_.isDirectory)
      .evalMap { packageDir =>
        IO {
          packageDir.listFiles.toList
            .filter(_.isDirectory)
            .map(_.getName)
            .maximumOption(Order.fromLessThan(Version.less))
            .map(version => packageDir.getName -> version)
        }
      }
      .unNone
  }

  protected def buildFsUrl(packageName: String, version: String): NioPath =
    HackagePackage(packageName, version).packageDir
}

object HaskellIndex {
  def apply(
      config: Config,
      db: Database,
      cindexDir: СindexDirectory
  )(
      implicit
      shift: ContextShift[IO],
      sourcesDownloader: SourcesDownloader[IO, HackagePackage]
  ) = new HaskellIndex(config.languagesConfig.haskell, db, cindexDir)
}
