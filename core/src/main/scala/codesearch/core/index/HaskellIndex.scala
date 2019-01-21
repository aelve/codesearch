package codesearch.core.index

import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.{Path => NioPath}

import ammonite.ops.{Path, pwd}
import cats.effect.{ContextShift, IO}
import cats.syntax.flatMap._
import codesearch.core.config.{Config, HaskellConfig}
import codesearch.core.db.HackageDB
import codesearch.core.index.repository.{Downloader, HackagePackage, SourcesDownloader}
import codesearch.core.index.directory.Directory._
import codesearch.core.index.directory.Directory.ops._
import codesearch.core.index.directory.СSearchDirectory
import codesearch.core.index.directory.СSearchDirectory.HaskellCSearchIndex
import codesearch.core.model.{HackageTable, Version}
import com.softwaremill.sttp.SttpBackend
import fs2.{Chunk, Stream}
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

import scala.sys.process._

class HaskellIndex(haskellConfig: HaskellConfig)(
    implicit val http: SttpBackend[IO, Stream[IO, ByteBuffer]],
    val shift: ContextShift[IO],
    downloader: Downloader[IO],
    sourcesDownloader: SourcesDownloader[IO, HackagePackage]
) extends LanguageIndex[HackageTable] with HackageDB {

  private val INDEX_LINK: String     = "http://hackage.haskell.org/packages/index.tar.gz"
  private val INDEX_SOURCE_GZ: Path  = pwd / 'data / 'meta / 'haskell / "index.tar.gz"
  private val INDEX_SOURCE_DIR: Path = pwd / 'data / 'meta / "haskell"

  override protected val csearchDir: СSearchDirectory = HaskellCSearchIndex

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

  override protected def getLastVersions: Stream[IO, (String, String)] = {
    Stream
      .evalUnChunk(IO(Chunk.array(INDEX_SOURCE_DIR.toIO.listFiles)))
      .filter(_.isDirectory)
      .evalMap { packageName =>
        IO {
          packageName.getName -> packageName.listFiles
            .filter(_.isDirectory)
            .map(_.getName)
            .max(Ordering.fromLessThan(Version.less))
        }
      }
  }

  override protected def buildFsUrl(packageName: String, version: String): NioPath =
    HackagePackage(packageName, version).packageDir
}

object HaskellIndex {
  def apply(config: Config)(
      implicit http: SttpBackend[IO, Stream[IO, ByteBuffer]],
      shift: ContextShift[IO],
      downloader: Downloader[IO],
      sourcesDownloader: SourcesDownloader[IO, HackagePackage]
  ) = new HaskellIndex(config.languagesConfig.haskellConfig)
}
