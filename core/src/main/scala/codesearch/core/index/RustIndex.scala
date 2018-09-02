package codesearch.core.index

import ammonite.ops.{Path, pwd}
import codesearch.core.db.CratesDB
import codesearch.core.index.LanguageIndex._
import codesearch.core.index.repository.CratesPackage
import repository.Extensions._
import codesearch.core.index.directory.Directory._
import codesearch.core.model
import codesearch.core.model.{CratesTable, Version}
import codesearch.core.util.Helper
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.sys.process._
import scala.concurrent.{ExecutionContext, Future}

class RustIndex(val ec: ExecutionContext) extends LanguageIndex[CratesTable] with CratesDB {

  override protected val logger: Logger    = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String = ".crates_csearch_index"
  override protected val langExts: String  = ".*\\.(rs)$"

  private val SOURCES: Path = pwd / 'data / 'rust / 'packages
  private val CARGO_PATH    = "./cargo"
  private val REPO_DIR      = pwd / 'data / 'rust / "crates.io-index"
  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

  override def csearch(args: SearchArguments, page: Int = 0): Future[CSearchPage] = {
    runCsearch(args).flatMap { answers =>
      verNames().map { verSeq =>
        val nameToVersion = Map(verSeq: _*)
        answers
          .slice(math.max(page - 1, 0) * LanguageIndex.PAGE_SIZE, page * LanguageIndex.PAGE_SIZE)
          .flatMap(uri => mapCSearchOutput(uri, nameToVersion))
          .groupBy(x => (x.name, x.url))
          .map {
            case ((name, packageLink), results) =>
              PackageResult(name, packageLink, results.map(_.result))
          }
          .toSeq
          .sortBy(_.name)
      }.map(CSearchPage(_, answers.length))
    }
  }

  override def downloadMetaInformation(): Unit = {
    s"git -C $REPO_DIR pull" !!
  }

  override protected def updateSources(name: String, version: String): Future[Int] = {
    archiveDownloadAndExtract(CratesPackage(name, version))
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val seq = Helper
      .recursiveListFiles(REPO_DIR.toIO)
      .collect {
        case file if !(IGNORE_FILES contains file.getName) =>
          val lastVersionJSON = scala.io.Source.fromFile(file).getLines().toSeq.last
          val obj             = Json.parse(lastVersionJSON)
          val name            = (obj \ "name").as[String]
          val vers            = (obj \ "vers").as[String]
          (name, model.Version(vers))
      }
      .toSeq
    Map(seq: _*)
  }

  private def mapCSearchOutput(uri: String, nameToVersion: Map[String, String]): Option[CSearchResult] = {
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
        case Some(packageName) =>
          nameToVersion.get(packageName).map { ver =>
            val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

            val remPath = pathSeq.drop(1).mkString("/")

            CSearchResult(packageName,
                          s"https://docs.rs/crate/$packageName/$ver",
                          CodeSnippet(
                            remPath,
                            firstLine,
                            nLine.toInt - 1,
                            rows
                          ))
          }
      }
    }
  }

  override protected def mapCSearchOutput(uri: String): Option[CSearchResult] = Option.empty
}

object RustIndex {
  def apply()(implicit ec: ExecutionContext) = new RustIndex(ec)
}
