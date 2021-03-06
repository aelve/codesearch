package codesearch.core.db

import java.sql.Timestamp

import cats.effect.IO
import codesearch.core.index.repository.SourcePackage
import codesearch.core.model._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DefaultDB[T <: DefaultTable] {

  def db: Database

  def table: TableQuery[T]

  def insertOrUpdate[A <: SourcePackage](pack: A): IO[Int] = {
    val insOrUpdate = table.insertOrUpdate(
      (
        pack.name,
        pack.version,
        new Timestamp(System.currentTimeMillis())
      ))
    IO.fromFuture(IO(db.run(insOrUpdate)))
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

  def verNames: IO[Seq[(String, String)]] = {
    val act = table
      .map(row => (row.packageName, row.lastVersion))
      .result
    IO.fromFuture(IO(db.run(act)))
  }

  def packageIsExists(packageName: String, packageVersion: String): IO[Boolean] = {
    val act = table
      .filter(p => p.packageName === packageName && p.lastVersion === packageVersion)
      .exists
      .result
    IO.fromFuture(IO(db.run(act)))
  }

  def verByName(packageName: String): Future[Option[String]] = {
    val act = table
      .filter(_.packageName === packageName)
      .map(_.lastVersion)
      .result
      .headOption
    db.run(act)
  }

  def initDB: IO[Unit] =
    IO.fromFuture(IO(db.run(MTable.getTables))).flatMap { vector =>
      IO(
        if (!vector.exists(_.name.name == table.baseTableRow.tableName))
          db.run(table.schema.create)
      )
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
