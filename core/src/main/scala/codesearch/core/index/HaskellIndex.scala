package codesearch.core.index

import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.{Path => NioPath}

import ammonite.ops.{Path, pwd}
import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, HaskellConfig}
import codesearch.core.db.HackageDB
import codesearch.core.index.repository.HackagePackage
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.repository.Extensions._
import codesearch.core.model.{HackageTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.Stream
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

import scala.concurrent.ExecutionContext
import scala.sys.process._

class HaskellIndex(haskellConfig: HaskellConfig)(
    implicit val executor: ExecutionContext,
    val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO]
) extends LanguageIndex[HackageTable] with HackageDB {

  override type Tag = Haskell

  override val csearchDir: СSearchDirectory[Tag] = implicitly

  private val INDEX_LINK: String     = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE_GZ: Path  = pwd / 'data / "index.tar.gz"
  private val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  override protected def concurrentTasksCount: Int = haskellConfig.concurrentTasksCount

  override protected def updateSources(name: String, version: String): IO[Int] = {
    logger.info(s"downloading package $name") >> archiveDownloadAndExtract(HackagePackage(name, version))
  }

  override def downloadMetaInformation: IO[Unit] =
    for {
      _ <- logger.info("update index")
      _ <- IO {
        val archive     = INDEX_SOURCE_GZ.toIO
        val destination = INDEX_SOURCE_DIR.toIO

        archive.getParentFile.mkdirs()
        destination.mkdirs()

        new URL(INDEX_LINK) #> archive !!

        val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
        archiver.extract(archive, destination)
      }
    } yield ()

  override protected def getLastVersions: Map[String, Version] = {
    val indexDir     = INDEX_SOURCE_DIR.toIO
    val packageNames = indexDir.listFiles.filter(_.isDirectory)
    val allVersions = packageNames.flatMap { packagePath =>
      packagePath.listFiles
        .filter(_.isDirectory)
        .map(versionPath => (packagePath.getName, Version(versionPath.getName)))
    }
    val lastVersions = allVersions.groupBy { case (name, _) => name }
      .mapValues(_.map { case (_, version) => version }.max)

    lastVersions
  }

  override protected def buildFsUrl(packageName: String, version: String): NioPath =
    HackagePackage(packageName, version).packageDir
}

object HaskellIndex {
  def apply(config: Config)(
      implicit ec: ExecutionContext,
      http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO]
  ) = new HaskellIndex(config.languagesConfig.haskellConfig)
}
