package codesearch.core.meta

trait MetaDownloader[F[_]] {

  /**
    * Download meta information about packages from remote repository
    * e.g. for Haskell is list of versions and cabal file for each version
    */
  def downloadMeta: F[Unit]
}
