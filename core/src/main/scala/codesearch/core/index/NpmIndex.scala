package codesearch.core.index

import java.io.FileInputStream

import sys.process._
import ammonite.ops.pwd
import codesearch.core.db.NpmDB
import codesearch.core.model.Version
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
}
