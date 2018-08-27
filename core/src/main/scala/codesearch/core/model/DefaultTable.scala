package codesearch.core.model

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._

// TODO: TIMESTAMP ON UPDATE NOW()
// TODO: UTC

class DefaultTable(tag: Tag, tableName: String) extends Table[(String, String, Timestamp)](tag, tableName) {
  def packageName = column[String](s"${tableName}_PACKAGE_NAME", O.PrimaryKey)
  def lastVersion = column[String](s"${tableName}_VERSION")

  def updated = column[Timestamp](s"${tableName}_UPDATED")

  def * = (packageName, lastVersion, updated)

  def indexTimestamps = index(s"${tableName}_LAST_UPDATED", updated)

}
