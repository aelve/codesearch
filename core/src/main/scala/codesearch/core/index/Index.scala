package codesearch.core.index

import codesearch.core.model.Version

trait Index {
  def updateIndex(): Unit
  def getLastVersions: Map[String, Version]
}
