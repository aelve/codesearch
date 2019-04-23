package codesearch.core.db.repository

import cats.Monad
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.Stream

final case class PackageIndex(
    name: String,
    version: String,
    repository: String
)

trait PackageIndexRep[F[_]] {
  def insertIndexes(packages: List[PackageIndex]): F[Int]
  def insertIndexes(stream: Stream[F, PackageIndex]): F[Int]
}

object PackageIndexRep {

  private val batchInsertQuery =
    """
      |INSERT INTO repository_index(name, version, repository)
      |  VALUES (?, ?, ?)
      |  ON CONFLICT (name, repository) DO UPDATE
      |  SET version = excluded.version
    """.stripMargin

  def apply[F[_]: Monad](xa: Transactor[F]): PackageIndexRep[F] = new PackageIndexRep[F] {

    def insertIndexes(packages: List[PackageIndex]): F[Int] =
      Update[PackageIndex](batchInsertQuery)
        .updateMany(packages)
        .transact(xa)

    def insertIndexes(stream: Stream[F, PackageIndex]): F[Int] = {
      val batchSize = 10000
      stream
        .chunkN(batchSize)
        .map(packages => insertIndexes(packages.toList))
        .compile
        .drain
    }
  }
}
