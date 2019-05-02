package codesearch.core.sources.ratelimiter

import scala.concurrent.duration.FiniteDuration

case class Rate(numberTasks: Int, duration: FiniteDuration)

trait RateLimiter {

}