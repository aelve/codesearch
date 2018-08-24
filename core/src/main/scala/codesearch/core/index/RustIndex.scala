package codesearch.core.index

import ammonite.ops.{Path, pwd}
import codesearch.core.model.CratesTable
import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process._
import scala.concurrent.{ExecutionContext, Future}

class RustIndex(val ec: ExecutionContext) extends LanguageIndex[CratesTable] {
  val SOURCES: Path = pwd / 'data / 'rust / 'packages

  private val CARGO_PATH = "./cargo"

  override val logger: Logger             = LoggerFactory.getLogger(this.getClass)
  override val indexAPI: CratesIndex.type = CratesIndex
  override val indexFile: String          = ".crates_csearch_index"
  override val langExts: String           = ".*\\.(rs)$"

  def csearch(searchQuery: String,
              insensitive: Boolean,
              precise: Boolean,
              sources: Boolean,
              page: Int = 0): Future[(Int, Seq[PackageResult])] = {
    val answers = runCsearch(searchQuery, insensitive, precise, sources)
    indexAPI.verNames().map { verSeq =>
      val nameToVersion = Map(verSeq: _*)
      (answers.length,
       answers
         .slice(math.max(page - 1, 0) * 100, page * 100)
         .flatMap(uri => indexAPI contentByURI (uri, nameToVersion))
         .groupBy(x => (x._1, x._2))
         .map {
           case ((name, packageLink), results) =>
             PackageResult(name, packageLink, results.map(_._3))
         }
         .toSeq
         .sortBy(_.name))
    }
  }

  def downloadSources(name: String, ver: String): Future[Int] = {
    SOURCES.toIO.mkdirs()

    try {
      s"rm -rf ${SOURCES / name}" !!

      s"$CARGO_PATH clone $name --vers $ver --prefix $SOURCES" !!

      logger.info("package cloned")

      val future = indexAPI.insertOrUpdate(name, ver)
      logger.info("DB updated")
      future
    } catch {
      case e: Exception =>
        Future[Int] {
          logger.info(e.getMessage)
          0
        }
    }
  }

  override implicit def executor: ExecutionContext = ec
}

object RustIndex {
  def apply()(implicit ec: ExecutionContext) = new RustIndex(ec)
}
