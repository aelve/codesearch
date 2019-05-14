package codesearch.core.search.engine

trait SearchProvider[F[_], QueryParam, Result] {
  def searchBy(param: QueryParam): F[Result]
}