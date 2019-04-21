package codesearch.core.db.repository

import java.time.LocalDateTime

import cats.Monad
import doobie.Transactor
import doobie.implicits._
import fs2.Stream

final case class PackageTableRow(
    name: String,
    version: String,
    repository: String,
    updatedAt: LocalDateTime
)

final case class Package(
    name: String,
    version: String
)

trait PackageRep[F[_]] {
  def findByRepository(repository: String): Stream[F, Package]
}

object PackageRep {
  def apply[F[_]: Monad](xa: Transactor[F]): PackageRep[F] = new PackageRep[F] {
    def findByRepository(repository: String): Stream[F, Package] = {
      sql"SELECT name, version FROM package WHERE repository = $repository"
        .query[Package]
        .stream
        .transact(xa)
    }
  }
}
