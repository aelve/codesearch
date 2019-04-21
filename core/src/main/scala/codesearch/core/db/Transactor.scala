package codesearch.core.db

import cats.effect.{Async, ContextShift, Resource}
import codesearch.core.config.DatabaseConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Transactor {
  def create[F[_]: Async: ContextShift](config: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    import config._
    for {
      connectEC     <- ExecutionContexts.fixedThreadPool[F](32)
      transactionEC <- ExecutionContexts.cachedThreadPool[F]
      xa <- HikariTransactor.newHikariTransactor(
        properties.driver,
        properties.url,
        user,
        password,
        connectEC,
        transactionEC
      )
    } yield xa
  }
}
