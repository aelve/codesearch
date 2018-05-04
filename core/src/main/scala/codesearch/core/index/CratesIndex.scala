package codesearch.core.index

import sys.process._
import ammonite.ops.pwd
import codesearch.core.db.{CratesDB, DefaultDB}
import codesearch.core.model.{CratesTable, Version}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._
import codesearch.core.util.Helper
import codesearch.core.model

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CratesIndex extends Index with CratesDB {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val REPO_DIR = pwd / 'data / 'rust / "crates.io-index"

  private val IGNORE_FILES = Set(
    "test-max-version-example-crate",
    "version-length-checking-is-overrated",
    "config.json",
    ".git"
  )

  override def updateIndex(): Unit = {
    s"git -C $REPO_DIR pull" !!
  }

  override def getLastVersions: Map[String, Version] = {
    val seq = Helper.recursiveListFiles(REPO_DIR.toIO).collect { case file if !(IGNORE_FILES contains file.getName) =>
      val lastVersionJSON = scala.io.Source.fromFile(file).getLines().toSeq.last
      println(s"$file :: $lastVersionJSON")
      val obj = Json.parse(lastVersionJSON)
      val name = (obj \ "name").as[String]
      val vers = (obj \ "vers").as[String]
      (name, model.Version(vers))
    }.toSeq
    Map(seq: _*)
  }

  def contentByURI(uri: String): Option[(String, Result)] = {
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
        case Some(packageName) =>
          Await.result(CratesIndex.verByName(packageName), Duration.Inf).map { verName => {
            val (firstLine, rows) = Helper.extractRows(fullPath, nLine.toInt)

            val remPath = pathSeq.drop(1).mkString("/")

            (verName, Result(
              s"https://docs.rs/crate/$packageName/$verName/source/$remPath",
              firstLine,
              nLine.toInt - 1,
              rows
            ))
          } }
      }
    }
  }
}
