package codesearch.core.syntax

import cats.Functor
import fs2.Stream
import cats.syntax.functor._

object stream {
  implicit class StreamOps[F[_], A](val stream: Stream[F, A]) {
    def filterM(f: A => F[Boolean])(implicit F: Functor[F]): Stream[F, A] =
      stream.evalMap(x => f(x).map(x -> _)).collect { case (value, true) => value }

    def filterNotM(f: A => F[Boolean])(implicit F: Functor[F]): Stream[F, A] =
      stream.evalMap(x => f(x).map(x -> _)).collect { case (value, false) => value }

    def filterNot(f: A => Boolean): Stream[F, A] = stream.filter(!f(_))
  }
}
