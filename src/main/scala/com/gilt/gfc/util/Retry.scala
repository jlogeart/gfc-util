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
  case class TooManyRetries[I](lastInput: I, wrapped: Option[Exception]) extends RuntimeException

  /**
    * Given an input I, retries a function that returns either a new I or an O, until it returns an O or a maximum number of retries has been reached.
    *
    * @param maxRetryTimes The maximum number of retries, defaults to Long.MaxValue. The function f is called at most maxRetryTimes + 1 times.
    *                      In other words, iff maxRetryTimes == 0, f will be called exactly once, iff maxRetryTimes == 1, it will be called at
    *                      most twice, etc.
    * @param i The initial input
    * @param f The function to (re)try
    * @param log An optional log function to report failed iterations to. By default prints the thrown Exception to the console.
    * @return A successful O if the function succeeded within maxRetryTimes or otherwise throws either TooManyRetries or the last thrown NonFatal Exception.
    *         If the function throws a fatal Error, it is not retried and the error is rethrown.
    */
  @tailrec
  def retryFold[I, O](maxRetryTimes: Long = Long.MaxValue)
                     (i: I)
                     (f: I => Either[I, O])
                     (implicit log: Throwable => Unit = _.printStackTrace): O = {
    (try f(i) catch {
      case e: Exception =>
        if (maxRetryTimes <= 0) {
          throw TooManyRetries(i, Some(e))
        }
        log(e)
        Left(i)
    }) match {
      case Left(i1) if maxRetryTimes <= 0 => throw TooManyRetries(i1, None)
      case Left(i1) => retryFold(maxRetryTimes - 1)(i1)(f)
      case Right(o) => o
    }
  }

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
  def retry[T](maxRetryTimes: Long = Long.MaxValue)
              (f: => T)
              (implicit log: Throwable => Unit = _.printStackTrace): T =
    try {
      retryFold(
        maxRetryTimes
      )(())(_ =>
        Right(f)
      )(log)
    } catch {
      case TooManyRetries(_, Some(e)) => throw e
    }

  /**
    * Given an input I, retries a function that returns either a new I or an O, until it returns an O, a maximum number of retries has been
    * reached, or a retry timeout has been reached. Each retry iteration is being exponentially delayed. The delay grows from a given
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
  def retryFoldWithExponentialDelay[I, O](maxRetryTimes: Long = Long.MaxValue,
                                          maxRetryTimeout: Deadline = 1 day fromNow,
                                          initialDelay: Duration = 1 millisecond,
                                          maxDelay: FiniteDuration = 1 day,
                                          exponentFactor: Double = 2)
                                         (i: I)
                                         (f: I => Either[I, O])
                                         (implicit log: Throwable => Unit = _.printStackTrace): O = {
    require(exponentFactor >= 1.0)
    val delay = Seq(initialDelay, maxDelay, maxRetryTimeout.timeLeft).min
    (try (f(i)) catch {
      case e: Exception =>
        if (maxRetryTimes <= 0 || maxRetryTimeout.isOverdue) {
          throw TooManyRetries(i, Some(e))
        }
        log(e)
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
        Left(i)
    }) match {
      case Left(i1) if maxRetryTimes <= 0 => throw TooManyRetries(i1, None)
      case Left(i1) => retryFoldWithExponentialDelay(maxRetryTimes - 1, maxRetryTimeout, delay * exponentFactor, maxDelay, exponentFactor)(i1)(f)(log)
      case Right(o) => o
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
  def retryWithExponentialDelay[T](maxRetryTimes: Long = Long.MaxValue,
                                   maxRetryTimeout: Deadline = 1 day fromNow,
                                   initialDelay: Duration = 1 millisecond,
                                   maxDelay: FiniteDuration = 1 day,
                                   exponentFactor: Double = 2)
                                  (f: => T)
                                  (implicit log: Throwable => Unit = _.printStackTrace): T =
    try {
      retryFoldWithExponentialDelay(
        maxRetryTimes, maxRetryTimeout, initialDelay, maxDelay, exponentFactor
      )(())(_ =>
        Right(f)
      )(log)
    } catch {
      case TooManyRetries(_, Some(e)) => throw e
    }
}
