package codesearch.core.db.repository

import cats.Monad
import doobie.Transactor
import doobie.implicits._
import fs2.Stream

final case class Package(
    name: String,
    version: String
)

final case class PackageTableRow(
    name: String,
    version: String,
    repository: String
)

trait PackageDbRepository[F[_]] {
  def upsert(`package`: PackageTableRow): F[Int]
  def findByRepository(repository: String): Stream[F, Package]
}

object PackageDbRepository {
  def apply[F[_]: Monad](xa: Transactor[F]): PackageDbRepository[F] = new PackageDbRepository[F] {
    def upsert(`package`: PackageTableRow): F[Int] = {
      sql"""
        INSERT INTO package(name, version, repository, updated_at)
        VALUES (${`package`.name}, ${`package`.version}, ${`package`.repository}, now())
        ON CONFLICT (name, repository) DO UPDATE
          SET version = excluded.version,
              updated_at = excluded.updated_at
        """.update.run.transact(xa)
    }

    def findByRepository(repository: String): Stream[F, Package] = {
      sql"""
        SELECT name, version 
        FROM package
        WHERE repository = $repository
      """.query[Package].stream.transact(xa)
    }
  }
}
