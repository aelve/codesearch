package codesearch.core.index

import java.io.FileInputStream
import java.nio.file.Path

import ammonite.ops.pwd
import codesearch.core.db.NpmDB
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.repository.Extensions._
import codesearch.core.index.repository.NpmPackage
import codesearch.core.model.{NpmTable, Version}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

class JavaScriptIndex(val ec: ExecutionContext) extends LanguageIndex[NpmTable] with NpmDB {

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".npm_csearch_index"
  override protected val langExts: String  = ".*\\.(js|json)$"

  private val NPM_INDEX_JSON     = pwd / 'data / 'js / "names.json"
  private val NPM_UPDATER_SCRIPT = pwd / 'scripts / "update_npm_index.js"

  override def downloadMetaInformation(): Unit = Seq("node", NPM_UPDATER_SCRIPT.toString) !!

  override protected def updateSources(name: String, version: String): Future[Int] = {
    archiveDownloadAndExtract(NpmPackage(name, version))
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(NPM_INDEX_JSON.toIO)
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
  def apply()(implicit ec: ExecutionContext) = new JavaScriptIndex(ec)
}
