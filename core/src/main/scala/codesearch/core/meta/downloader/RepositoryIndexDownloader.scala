package codesearch.core.meta.downloader

private[meta] trait RepositoryIndexDownloader[F[_]] {

    /**
      * Download meta information about packages from remote repository
      * e.g. for Haskell is list of versions and cabal file for each version
      */
    def download: F[Unit]
}
