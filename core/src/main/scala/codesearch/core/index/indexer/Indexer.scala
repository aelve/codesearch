package codesearch.core.index.indexer

import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}
import java.nio.file.{Files, Path => NioPath}

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.db.repository.{Package, PackageRep}
import codesearch.core.index.directory.{Directory, СindexDirectory}
import codesearch.core.syntax.path._
import doobie.util.transactor.Transactor
import fs2.io.file
import fs2.text.utf8Encode
import fs2.{Pipe, Stream}

import scala.sys.process.Process

private[indexer] trait Indexer[F[_]] {
  def index: F[Unit]
}

private[indexer] class SourcesIndexer[F[_]: Sync: ContextShift](
    indexDir: СindexDirectory,
    repository: String,
    xa: Transactor[F]
) extends Indexer[F] {

  def index: F[Unit] = {
    for {
      packageDirs <- latestPackagePaths
      _           <- createCSearchDir
      _           <- dropTempIndexFile
      _           <- dirsToIndex(packageDirs)
      _           <- indexPackages
      _           <- replaceIndexFile
    } yield ()
  }

  private def latestPackagePaths: F[Stream[F, NioPath]] = Sync[F].pure(
    PackageRep[F](xa)
      .findByRepository(repository)
      .through(buildFsPath)
  )

  private def buildFsPath: Pipe[F, Package, NioPath] = { input =>
    input.map(`package` => Directory.sourcesDir / repository / `package`.name / `package`.version)
  }

  private def dropTempIndexFile: F[Boolean] =
    Sync[F].delay(Files.deleteIfExists(indexDir.tempIndexDirAs[NioPath]))

  private def createCSearchDir: F[Option[NioPath]] = Sync[F].delay(
    if (Files.notExists(СindexDirectory.root))
      Some(Files.createDirectories(СindexDirectory.root))
    else None
  )

  private def dirsToIndex(stream: Stream[F, NioPath]): F[Unit] = {
    stream
      .map(_.toString + "\n")
      .through(utf8Encode)
      .through(file.writeAll(indexDir.dirsToIndex[NioPath], BlockingEC, List(CREATE, TRUNCATE_EXISTING)))
      .compile
      .drain
  }

  private def indexPackages: F[Unit] = {
    val args = Seq("cindex", indexDir.dirsToIndex[String])
    val env  = Seq("CSEARCHINDEX" -> indexDir.tempIndexDirAs[String])
    Sync[F].delay(Process(args, None, env: _*) !).void
  }

  private def replaceIndexFile: F[NioPath] =
    Sync[F].delay(
      Files.move(
        indexDir.tempIndexDirAs[NioPath],
        indexDir.indexDirAs[NioPath],
        REPLACE_EXISTING
      )
    )
}
