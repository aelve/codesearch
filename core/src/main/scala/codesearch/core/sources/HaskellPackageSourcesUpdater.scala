package codesearch.core.sources

final class HaskellPackageSourcesUpdater[F[_]] extends SourcesUpdater[F] {
    def update: F[Unit] =
}
