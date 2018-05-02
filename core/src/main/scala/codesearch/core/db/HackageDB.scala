package codesearch.core.db

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._
import codesearch.core.model._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.{Logger, LoggerFactory}

object HackageDB {
  private val logger: Logger = LoggerFactory.getLogger(HackageDB.getClass)

  val hackage = TableQuery[Hackage]
  val db = Database.forConfig("mydb")

  def insertOrUpdate(packageName: String, lastVersion: String): Future[Int] = {
    val insOrUpdate = hackage
      .insertOrUpdate((packageName, lastVersion, new Timestamp(System.currentTimeMillis())))
    db.run(insOrUpdate)
  }

  def updated: Future[Timestamp] = {
    val act = hackage
      .map(_.updated)
      .max
      .result
    db.run(act)
      .map(_.getOrElse(new Timestamp(0)))
  }

  def getSize: Future[Int] = {
    val act = hackage
      .size
      .result
    db.run(act)
  }

  def verNames(): Future[Seq[(String, String)]] = {
    val act = hackage
      .map(row => (row.packageName, row.lastVersion))
      .result
    db.run(act)
  }

  def verByName(packageName: String): Future[Option[String]] = {
    val act = hackage
      .filter(_.packageName === packageName)
      .map(_.lastVersion)
      .result
      .headOption
    db.run(act)
  }

  def initDB() = {
    db.run(hackage.schema.create)
  }
}
