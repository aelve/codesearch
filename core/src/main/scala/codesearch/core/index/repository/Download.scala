package codesearch.core.index.repository

import java.nio.file.Path

import cats.effect.IO
import simulacrum.typeclass

@typeclass trait Download[A] {
  def downloadSources(pack: A): IO[Path]
}
