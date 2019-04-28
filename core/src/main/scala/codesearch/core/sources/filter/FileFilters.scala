package codesearch.core.sources.filter

import cats.effect.Sync
import codesearch.core.config.{HaskellConfig, JavaScriptConfig, RubyConfig, RustConfig}
import codesearch.core.index.repository.Extensions.{
  HaskellExtensions,
  JavaScriptExtensions,
  RubyExtensions,
  RustExtensions
}

object HaskellFileFilter {
  def apply[F[_]: Sync](config: HaskellConfig): FileFilter[F] =
    FileFilter[F](HaskellExtensions, Set[String]())
}

object JavaScriptFileFilter {
  def apply[F[_]: Sync](config: JavaScriptConfig): FileFilter[F] =
    FileFilter(JavaScriptExtensions, Set[String]())
}

object RubyFileFilter {
  def apply[F[_]: Sync](config: RubyConfig): FileFilter[F] =
    FileFilter(RubyExtensions, Set[String]())
}

object RustFileFilter {
  def apply[F[_]: Sync](config: RustConfig): FileFilter[F] =
    FileFilter(RustExtensions, Set[String]())
}
