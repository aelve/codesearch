package codesearch.core.sources.unarchiver

import java.nio.file.Path

import cats.effect.Sync
import org.rauschig.jarchivelib.ArchiveFormat.TAR
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType.GZIP

object RubySourcesUnarchiver {
  def apply[F[_]: Sync]: SourcesUnarchiver[F] =
    (archive: Path, directory: Path) =>
      Sync[F].delay {
        val destDir    = directory.toFile
        val allowedSet = Set("tgz", "tar.gz")
        ArchiverFactory.createArchiver(TAR).extract(archive.toFile, destDir)
        destDir.listFiles
          .filter(file => allowedSet.exists(file.getName.toLowerCase.endsWith))
          .foreach(file => ArchiverFactory.createArchiver(TAR, GZIP).extract(file, destDir))
        directory
    }
}
