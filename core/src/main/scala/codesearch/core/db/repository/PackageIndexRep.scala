package codesearch.core.db.repository

import cats.Monad
import cats.implicits._
import doobie._
import doobie.implicits._

final case class PackageIndex(
    name: String,
    version: String,
    repository: String
)

trait PackageIndexRep[F[_]] {
  def insertRepIndexes(packages: List[PackageIndex]): F[Int]
}

object PackageIndexRep {

  private val batchInsertQuery =
    """
      |INSERT INTO repository_index(name, version, repository)
      |  VALUES (?, ?, ?)
      |  ON CONFLICT (name, repository) DO UPDATE
      |  SET version = excluded.version
    """.stripMargin

  def apply[F[_]: Monad](xa: Transactor[F]): PackageIndexRep[F] =
    (packages: List[PackageIndex]) =>
      Update[PackageIndex](batchInsertQuery)
        .updateMany(packages)
        .transact(xa)
}
