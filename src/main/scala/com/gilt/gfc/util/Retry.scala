package com.gilt.gfc.util

import java.util.concurrent.locks.LockSupport

import scala.annotation.tailrec
import scala.concurrent.duration._

/**
 * Helper to retry potentially failing functions
 *
 * @author Gregor Heine
 * @since 10/Apr/2015 16:55
 */
object Retry {
  /**
   * Retries a function until it succeeds or a maximum number of retries has been reached.
   *
   * @param maxRetryTimes The maximum number of retries, defaults to Long.MaxValue. The function f is called at most maxRetryTimes + 1 times.
   *                      In other words, iff maxRetryTimes == 0, f will be called exactly once, iff maxRetryTimes == 1, it will be called at
   *                      most twice, etc.
   * @param f The function to (re)try
   * @param log An optional log function to report failed iterations to. By default prints the thrown Exception to the console.
   * @return A successful T if the function succeeded within maxRetryTimes or the last thrown NonFatal Exception otherwise. If the function throws a fatal Error, it is not retried and the error is rethrown.
   */
  @tailrec
  def retry[T](maxRetryTimes: Long = Long.MaxValue)
              (f: => T)
              (implicit log: Throwable => Unit = _.printStackTrace): T = {
    try (f) catch {
      case e: Exception =>
        if (maxRetryTimes <= 0) {
          throw e
        }
        log(e)
        retry(maxRetryTimes - 1)(f)(log)
    }
  }

  /**
   * Retries a function until it succeeds, a maximum number of retries has been reached, or a retry timeout
   * has been reached. Each retry iteration is being exponentially delayed. The delay grows from a given
   * start value and by a given factor until it reaches a given maximum delay value. If maxRetryTimeout is reached,
   * the last function call is at the point of the timeout. E.g. if the initial delay is 1 second, the retry timeout 10 seconds
   * and all other parameters at their default, the function will be retried after 1, 3 (1+2), 7 (1+2+4) and finally 10 seconds before it fails.
   *
   * @param maxRetryTimes The maximum number of retries, defaults to Long.MaxValue. The function f is called at most maxRetryTimes + 1 times.
   *                      In other words, iff maxRetryTimes == 0, f will be called exactly once, iff maxRetryTimes == 1, it will be called at
   *                      most twice, etc.
   * @param maxRetryTimeout The retry Deadline until which to retry the function, defaults to 1 day from now
   * @param initialDelay The initial delay value, defaults to 1 nanosecond
   * @param maxDelay The maximum delay value, defaults to 1 day
   * @param exponentFactor The factor by which the delay increases between retry iterations
   * @param f The function to (re)try
   * @param log An optional log function to report failed iterations to. By default prints the thrown Exception to the console.
   * @return A successful T if the function succeeded within maxRetryTimes and maxRetryTimeout or the last thrown NonFatal Exception otherwise.
   *         If the function throws a fatal Error, it is not retried and the error is rethrown.
   */
  @tailrec
  def retryWithExponentialDelay[T](maxRetryTimes: Long = Long.MaxValue,
                                   maxRetryTimeout: Deadline = 1 day fromNow,
                                   initialDelay: Duration = 1 millisecond,
                                   maxDelay: FiniteDuration = 1 day,
                                   exponentFactor: Double = 2)
                                  (f: => T)
                                  (implicit log: Throwable => Unit = _.printStackTrace): T = {
    require(exponentFactor >= 1.0)
    try (f) catch {
      case e: Exception =>
        if (maxRetryTimes <= 0 || maxRetryTimeout.isOverdue) {
          throw e
        }
        log(e)
        val delay = Seq(initialDelay, maxDelay, maxRetryTimeout.timeLeft).min
        val delayNs = delay.toNanos
        // Under 10ms we use the more precise (but also more spurious) LockSupport.parkNanos, otherwise the more reliable Thread.sleep
        if (delayNs < 10000000L) {
          LockSupport.parkNanos(delayNs)
        } else {
          try {
            Thread.sleep(delayNs / 1000000L, (delayNs % 1000000).toInt)
          } catch {
            case ie: InterruptedException => /* ignore interrupted exceptions */
          }
        }
        retryWithExponentialDelay(maxRetryTimes - 1, maxRetryTimeout, delay * exponentFactor, maxDelay, exponentFactor)(f)(log)
    }
  }
}
