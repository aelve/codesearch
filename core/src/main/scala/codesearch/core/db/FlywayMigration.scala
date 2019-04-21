package codesearch.core.db

import cats.effect.Sync
import codesearch.core.config.DatabaseConfig
import org.flywaydb.core.Flyway

object FlywayMigration {
  def migrate[F[_]: Sync](config: DatabaseConfig): F[Unit] = Sync[F].delay {
    Flyway
      .configure()
      .dataSource(
        config.properties.url,
        config.user,
        config.password
      )
      .load()
      .migrate()
  }
}
