package codesearch.core.index

import ammonite.ops.pwd
import codesearch.core.db.CratesDB
import codesearch.core.index.repository.CratesPackage
import repository.Extensions._
import codesearch.core.index.directory.PackageDirectory._
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

  private val REPO_DIR = pwd / 'data / 'rust / "crates.io-index"
  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

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

  override protected def buildRepUrl(packageName: String, version: String): String =
    s"https://docs.rs/crate/$packageName/$version"
}

object RustIndex {
  def apply()(implicit ec: ExecutionContext) = new RustIndex(ec)
}
