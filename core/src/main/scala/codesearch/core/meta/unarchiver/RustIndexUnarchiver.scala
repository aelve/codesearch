package codesearch.core.meta.unarchiver

import java.io.InputStream
import java.util.zip.ZipInputStream

import cats.effect.{ConcurrentEffect, Sync}
import codesearch.core.config.LanguageConfig
import codesearch.core.db.repository.PackageIndex
import fs2.{Pipe, Stream}
import fs2.io.toInputStream
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveInputStream, ZipFile}

final class RustIndexUnarchiver[F[_]: ConcurrentEffect](
    config: LanguageConfig
) extends StreamIndexUnarchiver[F] {

  def packages: Pipe[F, Byte, PackageIndex] = { input =>
    input
      .through(toInputStream)
      .through(zipEntries)
      .through(flatPackages)
  }

  private def zipEntries: Pipe[F, InputStream, ZipArchiveEntry] = { input =>
    input.flatMap { inputStream =>
      Stream
        .bracket(Sync[F].delay(new ZipArchiveInputStream(inputStream)))(zis => Sync[F].delay(zis.close()))
        .flatMap { zipStream =>
          Stream.unfoldEval[F, ZipArchiveInputStream, ZipArchiveEntry](zipStream) { zipStream =>
            Sync[F].delay {
              zipStream.getNextZipEntry match {
                case entry: ZipArchiveEntry => Some(entry, zipStream)
                case _                      => None
              }
            }
          }
        }
    }
  }

  private def flatPackages: Pipe[F, ZipArchiveEntry, PackageIndex] = { input =>
  val a = ZipInputStream
    val b = ZipFile
      input.flatMap(_.)
    }

}

object RustIndexUnarchiver {
  def apply[F[_]: ConcurrentEffect](
      config: LanguageConfig
  ): RustIndexUnarchiver[F] = new RustIndexUnarchiver(config)
}
