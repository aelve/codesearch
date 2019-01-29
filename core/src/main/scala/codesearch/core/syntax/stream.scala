package codesearch.core.syntax
import cats.Functor
import fs2.Stream

object stream {
  implicit class StreamOps[F[_]: Functor, A](val stream: Stream[F, A]) {
    def filterM(f: A => F[Boolean]): Stream[F, A] =
      stream.zip(stream.evalMap(f)).collect { case (value, true) => value }

    def filterNotM(f: A => F[Boolean]): Stream[F, A] =
      stream.zip(stream.evalMap(f)).collect { case (value, false) => value }
  }
}
