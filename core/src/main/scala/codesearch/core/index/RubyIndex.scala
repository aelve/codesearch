package codesearch.core.index

import ammonite.ops.pwd
import codesearch.core.model.GemTable
import codesearch.core.util.Helper

import sys.process._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class RubyIndex(val ec: ExecutionContext) extends LanguageIndex[GemTable] {
  override val logger: Logger          = LoggerFactory.getLogger(this.getClass)
  override val indexAPI: GemIndex.type = GemIndex
  override val indexFile: String       = ".gem_csearch_index"
  override val langExts: String        = ".*\\.(rb)$"

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
      s"https://rubygems.org/downloads/$name-$ver.gem"

    val packageFileGZ =
      pwd / 'data / 'ruby / 'packages / name / ver / s"$ver.gem"

    val packageFileDir =
      pwd / 'data / 'ruby / 'packages / name / ver / ver

    archiveDownloadAndExtract(name,
                              ver,
                              packageURL,
                              packageFileGZ,
                              packageFileDir,
                              extensions = Some(Helper.langByExt.keySet),
                              extractor = gemExtractor)
  }

  def gemExtractor(src: String, dst: String): Unit = {
    Seq("gem", "unpack", s"--target=$dst", src) !!
  }

  override implicit def executor: ExecutionContext = ec
}

object RubyIndex {
  def apply()(implicit ec: ExecutionContext) = new RubyIndex(ec)
}
