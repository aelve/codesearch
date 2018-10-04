package codesearch.core.index.directory

import java.nio.file.{Path, Paths}

object PathOps {
  implicit final class RichPath(val parent: Path) {
    def /(child: Path): Path   = Paths.get(parent.toFile.getPath, child.toFile.getPath)
    def /(child: String): Path = Paths.get(parent.toFile.getPath, child)
  }
}
