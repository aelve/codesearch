package codesearch.core.index.repository

import java.nio.file.Path

import simulacrum.typeclass

import scala.concurrent.Future

@typeclass trait Download[A] {
  def downloadSources(pack: A): Future[Path]
}
