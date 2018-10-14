package codesearch

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

package object core {
  val BlockingEC: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}
