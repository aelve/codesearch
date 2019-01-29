package codesearch.core.syntax
import java.nio.file.{Path, Paths}

object path {
  implicit final class RichNioPath(private val parent: Path) {
    def /(child: Path): Path   = Paths.get(parent.toFile.getPath, child.toFile.getPath)
    def /(child: String): Path = Paths.get(parent.toFile.getPath, child)
  }
}
