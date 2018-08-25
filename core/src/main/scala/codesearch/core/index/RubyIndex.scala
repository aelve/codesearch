package codesearch.core.index

import java.io.FileInputStream
import java.net.URLDecoder

import ammonite.ops.pwd
import codesearch.core.db.GemDB
import codesearch.core.index.LanguageIndex.SearchArguments
import codesearch.core.model.{GemTable, Version}
import codesearch.core.util.Helper

import sys.process._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class RubyIndex(val ec: ExecutionContext) extends LanguageIndex[GemTable] with GemDB {

  override protected val logger: Logger          = LoggerFactory.getLogger(this.getClass)
  override protected val indexFile: String       = ".gem_csearch_index"
  override protected val langExts: String        = ".*\\.(rb)$"

  private val GEM_INDEX_URL = "http://rubygems.org/latest_specs.4.8.gz"
  private val GEM_INDEX_ARCHIVE = pwd / 'data / 'ruby / "ruby_index.gz"
  private val GEM_INDEX_JSON = pwd / 'data / 'ruby / "ruby_index.json"

  private val DESERIALIZER_PATH = pwd / 'codesearch / 'scripts / "update_index.rb"

  def csearch(args: SearchArguments, page: Int): (Int, Seq[PackageResult]) = {
    val answers = runCsearch(args)

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

  override protected def downloadSources(name: String, ver: String): Future[Int] = {
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

  override def downloadMetaInformation(): Unit = {
    Seq("curl", "-o", GEM_INDEX_ARCHIVE.toString, GEM_INDEX_URL) !!

    Seq("/usr/bin/ruby", DESERIALIZER_PATH.toString(),
      GEM_INDEX_ARCHIVE.toString(), GEM_INDEX_JSON.toString()) !!
  }

  def gemExtractor(src: String, dst: String): Unit = {
    Seq("gem", "unpack", s"--target=$dst", src) !!
  }

  override protected implicit def executor: ExecutionContext = ec

  override protected def getLastVersions: Map[String, Version] = {
    val stream = new FileInputStream(GEM_INDEX_JSON.toIO)
    val obj = Json.parse(stream).as[Seq[Seq[String]]]
    stream.close()
    obj.map { case Seq(name, ver, _) => (name, Version(ver)) }.toMap
  }

  private def contentByURI(uri: String): Option[(String, String, Result)] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      logger.warn(s"bad uri: $uri")
      None
    } else {
      val fullPath = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(6)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          logger.warn(s"bad uri: $uri")
          None
        case Some(name) =>
          val decodedName = URLDecoder.decode(name, "UTF-8")
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some((decodedName, s"https://rubygems.org/gems/$decodedName", Result(
            remPath,
            firstLine,
            nLine.toInt - 1,
            rows
          )))
      }
    }

  }
}

object RubyIndex {
  def apply()(implicit ec: ExecutionContext) = new RubyIndex(ec)
}
