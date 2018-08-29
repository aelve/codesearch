package codesearch.core.index

import java.io.FileInputStream
import java.net.URLDecoder

import ammonite.ops.{Path, pwd}
import codesearch.core.db.NpmDB
import codesearch.core.index.LanguageIndex.{CSearchResult, CodeSnippet}
import codesearch.core.index.repository.NpmPackage
import repository.Extensions._
import codesearch.core.index.directory.PackageDirectory._

import scala.sys.process._
import codesearch.core.model.{NpmTable, Version}
import codesearch.core.util.Helper
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class JavaScriptIndex(val ec: ExecutionContext) extends LanguageIndex[NpmTable] with NpmDB {

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".npm_csearch_index"
  override protected val langExts: String  = ".*\\.(js|json)$"

  private val SOURCES: Path           = pwd / 'data / 'js / 'packages
  private val NPM_INDEX_JSON          = pwd / 'data / 'js / "nameVersions.json"
  private val NPM_UPDATER_SCRIPT      = pwd / 'codesearch / 'scripts / "update_npm_index.js"
  private val extensions: Set[String] = Set("js", "json", "xml", "yml", "coffee", "markdown", "md", "yaml", "txt")

  private var counter: Int = 0

  override def downloadMetaInformation(): Unit = Seq("node", NPM_UPDATER_SCRIPT.toString) !!

  override protected def updateSources(name: String, version: String): Future[Int] = {
    counter += 1
    if (counter == 100) {
      counter = 0
      Thread.sleep(10000)
    }
    archiveDownloadAndExtract(NpmPackage(name, version))
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(NPM_INDEX_JSON.toIO)
    val obj    = Json.parse(stream).as[Seq[Map[String, String]]]
    stream.close()

    obj.map(map => (map.getOrElse("name", ""), Version(map.getOrElse("version", "")))).toMap
  }

  override protected def mapCSearchOutput(uri: String): Option[CSearchResult] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      println(s"bad uri: $uri")
      None
    } else {
      val fullPath             = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(6)
      val nLine                = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(name) =>
          val decodedName       = URLDecoder.decode(name, "UTF-8")
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some(
            CSearchResult(decodedName,
                          s"https://www.npmjs.com/package/$decodedName",
                          CodeSnippet(
                            remPath,
                            firstLine,
                            nLine.toInt - 1,
                            rows
                          )))
      }
    }
  }
}

object JavaScriptIndex {
  def apply()(implicit ec: ExecutionContext) = new JavaScriptIndex(ec)
}
