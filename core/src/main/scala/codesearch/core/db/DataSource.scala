package codesearch.core.db

import cats.effect.{Async, ContextShift, Resource, Sync}
import codesearch.core.config.DatabaseConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import javax.sql.DataSource
import org.flywaydb.core.Flyway

object DataSource {

  def migrate[F[_]: Sync](ds: DataSource): F[Unit] = Sync[F].delay {
    Flyway.configure().dataSource(ds).load().migrate()
  }

  def transactor[F[_]: Async: ContextShift](config: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    import config._
    for {
      connectEC     <- ExecutionContexts.fixedThreadPool[F](32)
      transactionEC <- ExecutionContexts.cachedThreadPool[F]
    } yield
      HikariTransactor.newHikariTransactor(
        config.properties.driver,
        config.properties.url,
        user,
        password,
        connectEC,
        transactionEC
      )
  }
}
