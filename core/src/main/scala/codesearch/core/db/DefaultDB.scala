package codesearch.core.db

import java.sql.Timestamp

import codesearch.core.index.repository.SourcePackage
import codesearch.core.model._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DefaultDB[T <: DefaultTable] {

  val table: TableQuery[T]
  val db = Database.forConfig("mydb")

  def insertOrUpdate[A <: SourcePackage](pack: A): Future[Int] = {
    val insOrUpdate = table.insertOrUpdate(
      pack.name,
      pack.version,
      new Timestamp(System.currentTimeMillis())
    )
    db.run(insOrUpdate)
  }

  def updated: Future[Timestamp] = {
    val act = table
      .map(_.updated)
      .max
      .result
    db.run(act)
      .map(_.getOrElse(new Timestamp(0)))
  }

  def getSize: Future[Int] = {
    val act = table.size.result
    db.run(act)
  }

  def verNames(): Future[Seq[(String, String)]] = {
    val act = table
      .map(row => (row.packageName, row.lastVersion))
      .result
    db.run(act)
  }

  def verByName(packageName: String): Future[Option[String]] = {
    val act = table
      .filter(_.packageName === packageName)
      .map(_.lastVersion)
      .result
      .headOption
    db.run(act)
  }

  def initDB(): Future[Unit] = {
    db.run(table.schema.create)
  }
}

trait HackageDB extends DefaultDB[HackageTable] {
  val table = TableQuery[HackageTable]
}

trait CratesDB extends DefaultDB[CratesTable] {
  val table = TableQuery[CratesTable]
}

trait NpmDB extends DefaultDB[NpmTable] {
  val table = TableQuery[NpmTable]
}

trait GemDB extends DefaultDB[GemTable] {
  val table = TableQuery[GemTable]
}

object HackageDB extends HackageDB
object CratesDB  extends CratesDB
object NpmDB     extends NpmDB
object GemDB     extends GemDB
