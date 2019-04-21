package codesearch.core.meta.unarchiver

import java.io.InputStream

import cats.effect.{ConcurrentEffect, Sync}
import codesearch.core.config.LanguageConfig
import codesearch.core.db.repository.PackageIndex
import fs2.io.toInputStream
import fs2.{Pipe, Stream}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}

final class HaskellIndexUnarchiver[F[_]: ConcurrentEffect](
    config: LanguageConfig
) extends StreamIndexUnarchiver[F] {

  def packages: Pipe[F, Byte, PackageIndex] = { input =>
    input
      .through(toInputStream)
      .through(tarEntries)
      .through(flatPackages)
  }

  private def tarEntries: Pipe[F, InputStream, TarArchiveEntry] = { input =>
    input.flatMap { inputStream =>
      Stream
        .bracket(Sync[F].delay(new TarArchiveInputStream(inputStream)))(tis => Sync[F].delay(tis.close()))
        .flatMap { tarStream =>
          Stream.unfoldEval[F, TarArchiveInputStream, TarArchiveEntry](tarStream) { tarStream =>
            Sync[F].delay {
              tarStream.getNextTarEntry match {
                case entry: TarArchiveEntry => Some(entry, tarStream)
                case _                      => None
              }
            }
          }
        }
    }
  }

  private def flatPackages: Pipe[F, TarArchiveEntry, PackageIndex] = { input =>
    input.flatMap { entry =>
      val parentName    = entry.getName
      val nestedEntries = entry.getDirectoryEntries
      Stream.emits(nestedEntries.map(nested => PackageIndex(parentName, nested.getName, config.repository)))
    }
  }
}

object HaskellIndexUnarchiver {
  def apply[F[_]: ConcurrentEffect](
      config: LanguageConfig
  ): HaskellIndexUnarchiver[F] = new HaskellIndexUnarchiver(config)
}
