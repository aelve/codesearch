package codesearch.core.syntax
import fs2.Stream

object stream {
  implicit class StreamOps[F[_], A](val stream: Stream[F, A]) extends AnyVal {
    def filterM(f: A => F[Boolean]): Stream[F, A] =
      stream.zip(stream.evalMap(f)).collect { case (value, true) => value }

    def filterNotM(f: A => F[Boolean]): Stream[F, A] =
      stream.zip(stream.evalMap(f)).collect { case (value, false) => value }
  }
}
