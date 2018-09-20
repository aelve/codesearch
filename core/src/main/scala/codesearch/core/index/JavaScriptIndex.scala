package codesearch.core.index

import java.nio.file.Path
import java.nio.ByteBuffer

import cats.effect.IO
import codesearch.core.db.NpmDB
import codesearch.core.index.details.NpmDetails
import codesearch.core.index.repository.NpmPackage
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.model.{NpmTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class JavaScriptIndex(
    private val ec: ExecutionContext,
    private val httpClient: SttpBackend[Future, Nothing],
    private implicit val streamHttp: SttpBackend[IO, Stream[IO, ByteBuffer]]
) extends LanguageIndex[NpmTable] with NpmDB {

  override protected implicit def executor: ExecutionContext         = ec
  override protected implicit def http: SttpBackend[Future, Nothing] = httpClient

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".npm_csearch_index"
  override protected val langExts: String  = ".*\\.(js|json)$"

  override def downloadMetaInformation(): Unit = {
    (for {
      _     <- IO(NpmDetails.FsIndexRoot.toFile.mkdirs())
      index <- NpmDetails().index
    } yield index).unsafeRunSync()
  }

  override protected def updateSources(name: String, version: String): Future[Int] =
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
      http: SttpBackend[Future, Nothing],
      streamHttp: SttpBackend[IO, Stream[IO, ByteBuffer]]
  ) = new JavaScriptIndex(ec, http, streamHttp)
}
