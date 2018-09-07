package codesearch.core.index

import java.nio.file.Path
import java.io.FileInputStream
import java.nio.file.Paths

import codesearch.core.db.NpmDB
import codesearch.core.index.repository.{FileDownloader, NpmPackage}
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.model.{NpmTable, Version}
import com.softwaremill.sttp.SttpBackend
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import com.softwaremill.sttp.{Uri, _}

import scala.concurrent.{ExecutionContext, Future}

class JavaScriptIndex(
    private val ec: ExecutionContext,
    private val sttp: SttpBackend[Future, Nothing]
) extends LanguageIndex[NpmTable] with NpmDB {

  override protected implicit def executor: ExecutionContext = ec
  override protected implicit def http: SttpBackend[Future, Nothing] = sttp

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".npm_csearch_index"
  override protected val langExts: String  = ".*\\.(js|json)$"

  private val NpmIndexJson = Paths.get("./data/js/names.json")
  private val NpmRegistryUrl = uri"https://replicate.npmjs.com/_all_docs?include_docs=true"

  override def downloadMetaInformation(): Unit = {
    val npmIndex = new FileDownloader().download(NpmRegistryUrl, NpmIndexJson)

  }

  override protected def updateSources(name: String, version: String): Future[Int] =
    archiveDownloadAndExtract(NpmPackage(name, version))

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(NpmIndexJson.toFile)
    val obj    = Json.parse(stream).as[Seq[Map[String, String]]]
    stream.close()

    obj.map(map => (map.getOrElse("name", ""), Version(map.getOrElse("version", "")))).toMap
  }

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://www.npmjs.com/package/$packageName/v/$version"

  override protected def buildFsUrl(packageName: String, version: String): Path =
    NpmPackage(packageName, version).packageDir
}

object JavaScriptIndex {
  def apply()(implicit ec: ExecutionContext, http: SttpBackend[Future, Nothing]) = new JavaScriptIndex(ec, http)
}
