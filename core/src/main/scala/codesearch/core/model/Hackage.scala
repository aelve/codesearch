package codesearch.core.db

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.math.Ordered.orderingToOrdered

case class Version(verString: String) extends Ordered[Version] {
  val version: Iterable[Int] = verString.split('.').map(_.toInt)

  override def compare(that: Version): Int = this.version compare that.version
}

class Hackage(tag: Tag) extends Table[(String, String,  Timestamp)](tag, "HACKAGE") {

//  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def packageName = column[String]("PACKAGE_NAME", O.PrimaryKey)
  def lastVersion = column[String]("LAST_VERSION")

  def updated = column[Timestamp]("LAST_UPDATE",
    SqlType("timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"))

  def * = (packageName, lastVersion, updated)
}
