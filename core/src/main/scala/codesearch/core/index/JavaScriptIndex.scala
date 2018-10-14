package codesearch.core.index

import java.nio.file.Path
import java.nio.ByteBuffer

import cats.effect.{ContextShift, IO}
import codesearch.core.db.NpmDB
import codesearch.core.index.details.NpmDetails
import codesearch.core.index.repository.NpmPackage
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.model.{NpmTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream

import scala.concurrent.ExecutionContext

class JavaScriptIndex(
    implicit val executor: ExecutionContext,
    val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO]
) extends LanguageIndex[NpmTable] with NpmDB {

  override protected val indexFile: String = ".npm_csearch_index"
  override protected val langExts: String  = ".*\\.(js|json)$"

  override def downloadMetaInformation: IO[Unit] =
    for {
      _     <- IO(NpmDetails.FsIndexRoot.toFile.mkdirs())
      index <- NpmDetails().index
    } yield index

  override protected def updateSources(name: String, version: String): IO[Int] =
    archiveDownloadAndExtract(NpmPackage(name, version))

  override protected def getLastVersions: Map[String, Version] = NpmDetails().detailsMap.unsafeRunSync()

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://www.npmjs.com/package/$packageName/v/$version"

  override protected def buildFsUrl(packageName: String, version: String): Path =
    NpmPackage(packageName, version).packageDir
}

object JavaScriptIndex {
  def apply()(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new JavaScriptIndex
}
