package com.gilt.gfc.util

import scala.concurrent.duration._

/**
 * To avoid tight loops around errors.
 */
@deprecated("Use the more flexible Retry functions", "0.1.0")
trait ExponentialBackoff {
  def error(ex: Throwable): Unit

  /**
   * Optional setting to wait a minimum time before a retry, defaulting to 1ms
   */
  protected def backoffMinTimeMs: Long = 1L

  /**
   * To be injected, max backoff time in millis.
   */
  protected def backoffMaxTimeMs: Long

  /**
   * Loops while loopCondition evaluates to true,
   * reports errors and backs off on error before next iteration
   * of the loop.
   */
  @deprecated("Use the more flexible Retry functions", "0.0.6")
  protected[this] final def loopWithBackoffOnErrorWhile(loopCondition: => Boolean)
                                                       (loopBody: => Unit) {
    var currentSleepTimeMs = backoffMinTimeMs

    def backoffOnError(sleepTimeMs: Long): Long = {
      try { Thread.sleep(sleepTimeMs) } catch { case ie: InterruptedException => /* ignore interrupted exceptions */ }

      val nextSleepTimeMs = 2L * sleepTimeMs
      if (nextSleepTimeMs > backoffMaxTimeMs) {
        backoffMaxTimeMs
      } else {
        nextSleepTimeMs
      }
    }

    while(loopCondition) {
      try {
        loopBody
      } catch {
        case e: Throwable =>
          error(e)
          currentSleepTimeMs = backoffOnError(currentSleepTimeMs)
      }
    }
  }

  /**
   * Loops forever until given operation succeeds.
   */
  protected[this] final def retry[T](operation: => T): T = {
    retryUpTo(Long.MaxValue)(operation)
  }

  /**
   * Loops until given operation succeeds up to maxTryTimes.
   */
  protected[this] final def retryUpTo[T](maxTryTimes: Long)
                                        (operation: => T): T = {
    Retry.retryWithExponentialDelay(maxRetryTimes = maxTryTimes - 1,
                                    initialDelay = backoffMinTimeMs millis,
                                    maxDelay = backoffMaxTimeMs millis)(
                                    operation)(
                                    error)
  }
}
