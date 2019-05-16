package codesearch.core.syntax
import java.nio.file.{Path, Paths}

object path {
  implicit final def string2Path(pathString: String): Path = Paths.get(pathString)
  implicit final class RichNioPath(private val parent: Path) extends AnyVal {
    def /(child: Path): Path   = Paths.get(parent.toFile.getPath, child.toFile.getPath)
    def /(child: String): Path = Paths.get(parent.toFile.getPath, child)
  }
}
