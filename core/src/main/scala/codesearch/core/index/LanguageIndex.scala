package codesearch.core.index

import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}
import java.nio.file.{Files, Path => NioPath}

import cats.effect.{ContextShift, Sync}
import cats.instances.int._
import cats.effect._
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.traverse._
import cats.syntax.functor._
import codesearch.core.BlockingEC
import codesearch.core.db.DefaultDB
import codesearch.core.index.directory.{Directory, СindexDirectory}
import codesearch.core.index.repository._
import codesearch.core.model.DefaultTable
import codesearch.core.syntax.stream._
import codesearch.core.util.manatki.syntax.raise._
import fs2.Stream
import fs2.Chunk
import fs2.io.file
import fs2.text.utf8Encode
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.sys.process.Process

trait LanguageIndex[F[_]: Sync] {

  protected implicit def shift: ContextShift[F]

  protected val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]

  protected def cindexDir: СindexDirectory

  protected def concurrentTasksCount: Int

  /**
    * Build new index from only latest version of each package and
    * replace old index with new one.
    */
  def buildIndex: F[Unit] = {
    def latestPackagePaths = verNames.map { versions =>
      versions.map {
        case (packageName, version) =>
          buildFsUrl(packageName, version)
      }
    }

    def dropTempIndexFile = F(Files.deleteIfExists(cindexDir.tempIndexDirAs[NioPath]))

    def createCSearchDir = (
      if (Files.notExists(СindexDirectory.root))
        Files.createDirectories(СindexDirectory.root)
    ).pure[F].widen

    def indexPackages(packageDirs: Seq[NioPath]): F[Unit] = {
      val args = Seq("cindex", cindexDir.dirsToIndex[String])
      val env  = Seq("CSEARCHINDEX" -> cindexDir.tempIndexDirAs[String])
      for {
        _ <- Stream
          .emits(packageDirs)
          .covary[F]
          .map(_.toString + "\n")
          .through(utf8Encode)
          .through(file.writeAll(cindexDir.dirsToIndex[NioPath], BlockingEC, List(CREATE, TRUNCATE_EXISTING)))
          .compile
          .drain
        _ <- (Process(args, None, env: _*) !).pure[F].widen
      } yield ()
    }

    def replaceIndexFile =
      Files.move(
        cindexDir.tempIndexDirAs[NioPath],
        cindexDir.indexDirAs[NioPath],
        REPLACE_EXISTING
    ).pure[F].widen

    for {
      packageDirs <- latestPackagePaths
      _           <- createCSearchDir
      _           <- dropTempIndexFile
      _           <- indexPackages(packageDirs)
      _           <- replaceIndexFile
    } yield ()
  }

  /**
    * download all latest packages version
    *
    * @return count of updated packages
    */
  def updatePackages(limit: Option[Int]): F[Int] = {
    val chunkSize = 10000
    val packages: Stream[F, (String, String)] = getLastVersions.chunkN(chunkSize).flat.filterNotM {
      case (packageName, packageVersion) =>
        packageIsExists(packageName, packageVersion)
    }

    logger.debug("UPDATE PACKAGES") >> limit
      .map(packages.take(_))
      .getOrElse(packages)
      .mapAsyncUnordered(concurrentTasksCount)(updateSources _ tupled)
      .compile
      .foldMonoid
  }

  /**
    * Create path to package in file system
    *
    * @param packageName local package name
    * @param version     of package
    * @return path
    */
  protected def buildFsUrl(packageName: String, version: String): NioPath

  protected def archiveDownloadAndExtract[B <: SourcePackage: Directory](pack: B)(
      implicit repository: SourcesDownloader[F, B]
  ): F[Int] = {
    val task = for {
      _         <- repository.downloadSources(pack)
      rowsCount <- insertOrUpdate(pack)
    } yield rowsCount
    task.handleErrorWith(ex => logger.error(ex)(s"Error while downloading $pack") as 0)
  }

  /**
    * Collect last versions of packages in local folder
    * Key for map is package name, value is last version
    *
    * @return last versions of packages
    */
  protected def getLastVersions: Stream[F, (String, String)]

  /**
    * Update source code from remote repository
    *
    * @see [[https://github.com/aelve/codesearch/wiki/Codesearch-developer-Wiki#updating-packages]]
    * @param name    of package
    * @param version of package
    * @return count of downloaded files (source files)
    */
  protected def updateSources(name: String, version: String): F[Int]
}

case class BadExitCode(code: Int) extends Exception(s"Process returned a bad exit code: $code")
