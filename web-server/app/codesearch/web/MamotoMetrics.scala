package codesearch.web

import cats.effect.IO
import codesearch.core.config.Config

object MamotoMetrics {
  val isEnabled: Boolean = Config
    .load[IO]
    .map(_.metrics)
    .unsafeRunSync()
    .enableMamotoMetrics
}
