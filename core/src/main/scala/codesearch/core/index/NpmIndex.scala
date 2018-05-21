package codesearch.core.index

import java.io.FileInputStream
import java.net.URLDecoder

import sys.process._
import ammonite.ops.pwd
import codesearch.core.db.NpmDB
import codesearch.core.model.Version
import codesearch.core.util.Helper
import play.api.libs.json.Json

import scala.collection.mutable

object NpmIndex extends Index with NpmDB {
  private val NPM_INDEX_JSON = pwd / 'data / 'js / "nameVersions.json"
  private val NPM_UPDATER_SCRIPT = pwd / 'codesearch / 'scripts / "update_npm_index.js"

  override def updateIndex(): Unit = {
    Seq("node", NPM_UPDATER_SCRIPT.toString) !!
  }

  override def getLastVersions: Map[String, Version] = {
    val obj = Json.parse(new FileInputStream(NPM_INDEX_JSON.toIO)).as[Seq[Map[String, String]]]
    val result = mutable.Map.empty[String, Version]
    obj.foreach { map =>
      result.update(map.getOrElse("name", ""), Version(map.getOrElse("version", "")))
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
      val pathSeq: Seq[String] = elems.head.split('/').drop(6)
      val nLine = elems.drop(1).head
      pathSeq.headOption match {
        case None =>
          println(s"bad uri: $uri")
          None
        case Some(name) =>
          val decodedName = URLDecoder.decode(name, "UTF-8")
          val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

          val remPath = pathSeq.drop(1).mkString("/")

          Some((decodedName, s"https://hackage.haskell.org/package/$decodedName", Result(
            remPath,
            firstLine,
            nLine.toInt - 1,
            rows
          )))
      }
    }

  }
}
