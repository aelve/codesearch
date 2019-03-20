package codesearch.core.index

import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core._
import codesearch.core.config.{Config, RubyConfig}
import codesearch.core.db.GemDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.RubyCSearchIndex
import codesearch.core.index.repository.{GemPackage, SourcesDownloader}
import codesearch.core.model.GemTable
import io.circe.fs2._
import fs2.Stream
import fs2.io.file
import slick.jdbc.PostgresProfile.api._

class RubyIndex(rubyConfig: RubyConfig, val db: Database)(
    implicit val shift: ContextShift[IO],
    sourcesDownloader: SourcesDownloader[IO, GemPackage]
) extends LanguageIndex[GemTable] with GemDB {

  override protected val csearchDir: СSearchDirectory = RubyCSearchIndex

  override protected def concurrentTasksCount: Int = rubyConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(GemPackage(name, version))
  }

  override protected def getLastVersions: Stream[IO, (String, String)] = {
    file
      .readAll[IO](rubyConfig.repoJsonPath, BlockingEC, 4096)
      .through(byteArrayParser[IO])
      .through(decoder[IO, Seq[String]])
      .collect { case Seq(name, version, _) => name -> version }
  }

  override protected def buildFsUrl(packageName: String, version: String): Path =
    GemPackage(packageName, version).packageDir
}

object RubyIndex {
  def apply(config: Config, db: Database)(
      implicit shift: ContextShift[IO],
      sourcesDownloader: SourcesDownloader[IO, GemPackage]
  ) = new RubyIndex(config.languagesConfig.ruby, db)
}
