package codesearch.core.model

import slick.jdbc.PostgresProfile.api._

class HackageTable(tag: Tag) extends DefaultTable(tag, "HACKAGE") {}
