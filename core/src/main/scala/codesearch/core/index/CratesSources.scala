package codesearch.core.index

import ammonite.ops.{Path, pwd}
import codesearch.core.model.CratesTable
import codesearch.core.util.Helper
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.Future

object CratesSources extends Sources[CratesTable] {
  val SOURCES: Path = pwd / 'data / 'rust / 'packages

  private val CARGO_PATH = "./cargo"

  override val logger: Logger = LoggerFactory.getLogger(CratesSources.getClass)
  override val indexAPI: CratesIndex.type = CratesIndex

  def csearch(searchQuery: String, insensitive: Boolean, precise: Boolean, sources: Boolean, page: Int = 0): Future[(Int, Seq[PackageResult])] = {
    val query: String = {
      if (precise) {
        Helper.hideSymbols(searchQuery)
      } else {
        searchQuery
      }
    }

    val args: mutable.ListBuffer[String] = mutable.ListBuffer("csearch", "-n")
    if (insensitive) {
      args.append("-i")
    }
    if (sources) {
      args.append("-f", ".*\\.(rs)$")
    }
    args.append(query)

    val answer = (args #| Seq("head", "-1001")).!!

    indexAPI.verNames().map { verSeq =>
      val nameToVersion = Map(verSeq: _*)
      val answers = answer.split('\n')
      (answers.length, answers
        .slice(math.max(page - 1, 0) * 100, page * 100)
        .flatMap(uri => indexAPI contentByURI(uri, nameToVersion)).groupBy(x => (x._1, x._2)).map {
        case ((name, packageLink), results) =>
          PackageResult(name, packageLink, results.map(_._3))
      }.toSeq.sortBy(_.name))
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
}
