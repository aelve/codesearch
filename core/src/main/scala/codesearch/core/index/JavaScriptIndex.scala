package codesearch.core.index

import java.io.FileInputStream
import java.net.{URLDecoder, URLEncoder}

import ammonite.ops.{Path, pwd}
import codesearch.core.db.NpmDB

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

  def csearch(searchQuery: String,
              insensitive: Boolean,
              precise: Boolean,
              sources: Boolean,
              page: Int): (Int, Seq[PackageResult]) = {
    val answers = runCsearch(searchQuery, insensitive, precise, sources)
    (answers.length,
     answers
       .slice(math.max(page - 1, 0) * 100, page * 100)
       .flatMap(contentByURI)
       .groupBy { x =>
         (x._1, x._2)
       }
       .map {
         case ((verName, packageLink), results) =>
           PackageResult(verName, packageLink, results.map(_._3).toSeq)
       }
       .toSeq
       .sortBy(_.name))
  }

  override def downloadMetaInformation(): Unit = Seq("node", NPM_UPDATER_SCRIPT.toString) !!

  override protected def downloadSources(name: String, ver: String): Future[Int] = {
    counter += 1
    if (counter == 100) {
      counter = 0
      Thread.sleep(10000)
    }
    val encodedName = URLEncoder.encode(name, "UTF-8")
    SOURCES.toIO.mkdirs()

    s"rm -rf ${SOURCES / encodedName}" !!

    val packageURL =
      s"https://registry.npmjs.org/$name/-/$name-$ver.tgz"

    val packageFileGZ =
      pwd / 'data / 'js / 'packages / encodedName / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'js / 'packages / encodedName / ver

    logger.info(s"EXTRACTING $name-$ver (dir: $encodedName)")
    val result = archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir, Some(extensions))
    result
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(NPM_INDEX_JSON.toIO)
    val obj    = Json.parse(stream).as[Seq[Map[String, String]]]
    stream.close()

    obj.map(map => (map.getOrElse("name", ""), Version(map.getOrElse("version", "")))).toMap
  }

  private def contentByURI(uri: String): Option[(String, String, Result)] = {
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
            (decodedName,
             s"https://www.npmjs.com/package/$decodedName",
             Result(
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
