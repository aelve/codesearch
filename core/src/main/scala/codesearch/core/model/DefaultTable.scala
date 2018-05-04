package codesearch.core.model

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

// TODO: TIMESTAMP ON UPDATE NOW()
// TODO: UTC

class DefaultTable(tag: Tag, tableName: String)
  extends Table[(String, String,  Timestamp)](tag, tableName) {
  def packageName = column[String]("PACKAGE_NAME", O.PrimaryKey)
  def lastVersion = column[String]("LAST_VERSION")

  def updated = column[Timestamp]("LAST_UPDATE")

  def * = (packageName, lastVersion, updated)

  def indexTimestamps = index("INDEX_UPDATED", updated)

}
