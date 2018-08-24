package codesearch.core.index

import ammonite.ops.pwd
import codesearch.core.model.HackageTable
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

case class Result(fileLink: String, firstLine: Int, nLine: Int, ctxt: Seq[String])
case class PackageResult(name: String, packageLink: String, results: Seq[Result])

class HaskellIndex(val ec: ExecutionContext) extends LanguageIndex[HackageTable] {
  override val logger: Logger              = LoggerFactory.getLogger(this.getClass)
  override val indexAPI: HackageIndex.type = HackageIndex
  override val indexFile: String           = ".hackage_csearch_index"
  override val langExts: String            = ".*\\.(hs|lhs|hsc|hs-boot|lhs-boot)$"

  def csearch(searchQuery: String,
              insensitive: Boolean,
              precise: Boolean,
              sources: Boolean,
              page: Int): (Int, Seq[PackageResult]) = {
    val answers = runCsearch(searchQuery, insensitive, precise, sources)
    (answers.length,
     answers
       .slice(math.max(page - 1, 0) * 100, page * 100)
       .flatMap(indexAPI.contentByURI)
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

  def downloadSources(name: String, ver: String): Future[Int] = {
    logger.info(s"downloading package $name")

    val packageURL =
      s"https://hackage.haskell.org/package/$name-$ver/$name-$ver.tar.gz"

    val packageFileGZ =
      pwd / 'data / 'packages / name / ver / s"$ver.tar.gz"

    val packageFileDir =
      pwd / 'data / 'packages / name / ver / ver

    archiveDownloadAndExtract(name, ver, packageURL, packageFileGZ, packageFileDir)
  }

  override implicit def executor: ExecutionContext = ec
}

object HaskellIndex {
  def apply()(implicit ec: ExecutionContext) = new HaskellIndex(ec)
}
