package codesearch.core.index

import ammonite.ops.{Path, pwd}
import codesearch.core.db.CratesDB
import codesearch.core.index.LanguageIndex.SearchArguments
import codesearch.core.model
import codesearch.core.model.{CratesTable, Version}
import codesearch.core.util.Helper
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.sys.process._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RustIndex(val ec: ExecutionContext) extends LanguageIndex[CratesTable] with CratesDB {

  override protected val logger: Logger             = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String          = ".crates_csearch_index"
  override protected val langExts: String           = ".*\\.(rs)$"

  private val SOURCES: Path = pwd / 'data / 'rust / 'packages
  private val CARGO_PATH = "./cargo"
  private val REPO_DIR = pwd / 'data / 'rust / "crates.io-index"
  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

  def csearch(args: SearchArguments, page: Int = 0): Future[(Int, Seq[PackageResult])] = {
    val answers = runCsearch(args)
    verNames().map { verSeq =>
      val nameToVersion = Map(verSeq: _*)
      (answers.length,
       answers
         .slice(math.max(page - 1, 0) * 100, page * 100)
         .flatMap(uri => contentByURI (uri, nameToVersion))
         .groupBy(x => (x._1, x._2))
         .map {
           case ((name, packageLink), results) =>
             PackageResult(name, packageLink, results.map(_._3))
         }
         .toSeq
         .sortBy(_.name))
    }
  }

  override def downloadMetaInformation(): Unit = {
    s"git -C $REPO_DIR pull" !!
  }

  override protected def downloadSources(name: String, ver: String): Future[Int] = {
    SOURCES.toIO.mkdirs()
    logger.info(s"downloading package $name $ver")
    Future {
      s"rm -rf ${SOURCES / name}" !!

      s"$CARGO_PATH clone $name --vers $ver --prefix $SOURCES" !!

    }.flatMap(_ => insertOrUpdate(name, ver)).transform( t => t.toEither match {
      case Left(e) =>
        logger.error(s"failed download package $name $ver", e)
        Try(0)
      case Right(v) => Try(v)
    })
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val seq = Helper.recursiveListFiles(REPO_DIR.toIO).collect { case file if !(IGNORE_FILES contains file.getName) =>
      val lastVersionJSON = scala.io.Source.fromFile(file).getLines().toSeq.last
      val obj = Json.parse(lastVersionJSON)
      val name = (obj \ "name").as[String]
      val vers = (obj \ "vers").as[String]
      (name, model.Version(vers))
    }.toSeq
    Map(seq: _*)
  }

  private def contentByURI(uri: String, nameToVersion: Map[String, String]): Option[(String, String, Result)] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      println(s"bad uri: $uri")
      None
    } else {
      val fullPath = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(6)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(packageName) => nameToVersion.get(packageName).map{ ver =>
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          (packageName, s"https://docs.rs/crate/$packageName/$ver", Result(
            remPath,
            firstLine,
            nLine.toInt - 1,
            rows
          ))
        }
      }
    }
  }
}

object RustIndex {
  def apply()(implicit ec: ExecutionContext) = new RustIndex(ec)
}
