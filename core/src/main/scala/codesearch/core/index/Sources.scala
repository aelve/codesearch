package codesearch.core.index

import codesearch.core.db.DefaultDB
import codesearch.core.model.DefaultTable

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Sources[VTable <: DefaultTable] {
  protected val indexAPI: Index with DefaultDB[VTable]

  def downloadSources(name: String, ver: String): Future[Int]

  def update(): Future[Int] = {
    val lastVersions = indexAPI.getLastVersions.mapValues(_.verString)

    val futureAction = indexAPI.verNames().flatMap { packages =>
      val packagesMap = Map(packages: _*)
      Future.sequence(lastVersions.filter {
        case (packageName, currentVersion) =>
          !packagesMap.get(packageName).contains(currentVersion)
      }.map {
        case (packageName, currentVersion) =>
          downloadSources(packageName, currentVersion)
      })
    }.map(_.sum)

    futureAction
  }

}
