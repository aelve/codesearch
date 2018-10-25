package codesearch.core.index.directory
import java.nio.file.{Path, Paths}

private[index] object Preamble {
  implicit final class RichNioPath(private val parent: Path) {
    def /(child: Path): Path   = Paths.get(parent.toFile.getPath, child.toFile.getPath)
    def /(child: String): Path = Paths.get(parent.toFile.getPath, child)
  }
}
