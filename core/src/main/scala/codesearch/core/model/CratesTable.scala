package codesearch.core.model

import slick.jdbc.PostgresProfile.api._

class CratesTable(tag: Tag) extends DefaultTable(tag, "CRATES")
