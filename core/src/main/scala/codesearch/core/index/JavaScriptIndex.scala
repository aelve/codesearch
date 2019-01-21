package codesearch.core.index

import java.nio.ByteBuffer
import java.nio.file.Path

import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, JavaScriptConfig}
import codesearch.core.db.NpmDB
import codesearch.core.index.details.NpmDetails
import codesearch.core.index.repository.{Downloader, NpmPackage, SourcesDownloader}
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.JavaScriptCSearchIndex
import codesearch.core.model.NpmTable
import com.softwaremill.sttp.SttpBackend
import fs2.Stream

class JavaScriptIndex(javaScriptConfig: JavaScriptConfig)(
    implicit val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO],
    downloader: Downloader[IO],
    sourcesDownloader: SourcesDownloader[IO, NpmPackage]
) extends LanguageIndex[NpmTable] with NpmDB {

  override protected val csearchDir: СSearchDirectory = JavaScriptCSearchIndex

  override protected def concurrentTasksCount: Int = javaScriptConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(NpmPackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] =
    for {
      _     <- IO(NpmDetails.FsIndexRoot.toFile.mkdirs())
      index <- NpmDetails().index
    } yield index

  override protected def getLastVersions: Stream[IO, (String, String)] = NpmDetails().detailsMap

  override protected def buildFsUrl(packageName: String, version: String): Path =
    NpmPackage(packageName, version).packageDir
}

object JavaScriptIndex {
  def apply(config: Config)(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO],
      downloader: Downloader[IO],
      sourcesDownloader: SourcesDownloader[IO, NpmPackage]
  ) = new JavaScriptIndex(config.languagesConfig.javaScriptConfig)
}
