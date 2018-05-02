package codesearch.core.model

import java.sql.Timestamp
import scala.math.Ordered.orderingToOrdered
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

case class Version(verString: String) extends Ordered[Version] {
  val version: Iterable[Int] = verString.split('.').map(_.toInt)

  override def compare(that: Version): Int = this.version compare that.version
}

object Version {
  def less(ver1: String, ver2: String): Boolean = Version(ver1) < Version(ver2)
}

class Hackage(tag: Tag) extends Table[(String, String,  Timestamp)](tag, "HACKAGE") {

  def packageName = column[String]("PACKAGE_NAME", O.PrimaryKey)
  def lastVersion = column[String]("LAST_VERSION")

  def updated = column[Timestamp]("LAST_UPDATE")

  def * = (packageName, lastVersion, updated)

  def indexTimestamps = index("INDEX_UPDATED", updated)
}
