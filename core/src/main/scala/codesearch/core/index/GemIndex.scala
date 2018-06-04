package codesearch.core.index

import java.io.FileInputStream

import sys.process._
import ammonite.ops.pwd
import codesearch.core.db.GemDB
import codesearch.core.model.Version
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._
import codesearch.core.util.Helper

import scala.collection.mutable

object GemIndex extends Index with GemDB {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val GEM_INDEX_URL = "http://rubygems.org/latest_specs.4.8.gz"
  private val GEM_INDEX_ARCHIVE = pwd / 'data / 'ruby / "ruby_index.gz"
  private val GEM_INDEX_JSON = pwd / 'data / 'ruby / "ruby_index.json"

  private val DESERIALIZER_PATH = pwd / 'codesearch / 'scripts / "update_index.rb"

  override def updateIndex(): Unit = {
    Seq("curl", "-o", GEM_INDEX_ARCHIVE.toString, GEM_INDEX_URL) !!

    Seq("/usr/bin/ruby", DESERIALIZER_PATH.toString(),
      GEM_INDEX_ARCHIVE.toString(), GEM_INDEX_JSON.toString()) !!
  }

  override def getLastVersions: Map[String, Version] = {
    val obj = Json.parse(new FileInputStream(GEM_INDEX_JSON.toIO)).as[Seq[Seq[String]]]
    val result = mutable.Map.empty[String, Version]
    obj.foreach {
      case Seq(name, ver, _) =>
        result.update(name, Version(ver))
    }
    result.toMap
  }

  def contentByURI(uri: String): Option[(String, String, Result)] = {
    val elems: Seq[String] = uri.split(':')
    if (elems.length < 2) {
      println(s"bad uri: $uri")
      None
    } else {
      val fullPath = elems.head
      val pathSeq: Seq[String] = elems.head.split('/').drop(8)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(name) =>
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some((name, s"https://hackage.haskell.org/package/$name", Result(
            remPath,
            firstLine,
            nLine.toInt - 1,
            rows
          )))
      }
    }
  }
}
