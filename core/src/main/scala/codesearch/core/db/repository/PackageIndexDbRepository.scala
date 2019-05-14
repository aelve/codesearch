package codesearch.core.db.repository

import cats.Monad
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.Stream

final case class PackageIndexTableRow(
    name: String,
    version: String,
    repository: String
)

final case class PackageIndex(
    name: String,
    version: String
)

trait PackageIndexRepository[F[_]] {
  def batchUpsert(packages: List[PackageIndexTableRow]): F[Int]
  def batchUpsert(stream: Stream[F, PackageIndexTableRow]): F[Int]
  def findLatestByRepository(repository: String): Stream[F, PackageIndexTableRow]
}

object PackageIndexDbRepository {
  def apply[F[_]: Monad](xa: Transactor[F]): PackageIndexRepository[F] = new PackageIndexRepository[F] {

    def batchUpsert(packages: List[PackageIndexTableRow]): F[Int] = {
      Update[PackageIndexTableRow](
        """
          |INSERT INTO repository_index(name, version, repository)
          |VALUES (?, ?, ?)
          |ON CONFLICT (name, repository) DO UPDATE
          | SET version = excluded.version
        """.stripMargin
      ).updateMany(packages).transact(xa)
    }

    def batchUpsert(stream: Stream[F, PackageIndexTableRow], batchSize: Int = 10000): F[Int] = {
      stream
        .chunkN(batchSize)
        .map(packages => batchUpsert(packages.toList))
        .compile
        .drain
    }

    def findLatestByRepository(repository: String): Stream[F, PackageIndexTableRow] = {
      sql"""
        SELECT r.name, r.version, r.repository
        FROM repository_index r
          LEFT JOIN package p
            ON r.name <> p.name AND r.version <> p.version
      """.query[PackageIndexTableRow].stream.transact(xa)
    }
  }
}
