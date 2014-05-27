package com.gilt.gfc.util

/**
 * To avoid tight loops around errors.
 */
trait ExponentialBackoff extends Loggable {

  /**
   * To be injected, max backoff time in millis.
   */
  protected def backoffMaxTimeMs: Long

  private[this] var currentSleepTimeMs = 1L

  /**
   * Loops while loopCondition evaluates to true,
   * reports errors and backs off on error before next iteration
   * of the loop.
   */
  protected[this] final def loopWithBackoffOnErrorWhile(loopCondition: => Boolean)
                                                       (loopBody: => Unit) {
    while(loopCondition) {
      try {
        loopBody
        backoffReset() // successful iteration
      } catch {
        case e: Throwable =>
          error(e.getMessage, e)
          backoffOnError()
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
   * Loops until given operation succeeds up to maxRetryTimes.
   */
  protected[this] final def retryUpTo[T](maxRetryTimes: Long)
                                        (operation: => T): T = {
    var numRetries = 0L
    var result = Option.empty[T]

    loopWithBackoffOnErrorWhile(!result.isDefined && (numRetries < maxRetryTimes)) {
      numRetries += 1
      result = Some(operation)
    }

    result.getOrElse {
      sys.error("Max number of retries reached [%d], operation aborted".format(maxRetryTimes))
    }
  }

  protected[this] final def backoffReset() {
    currentSleepTimeMs = 1L
  }

  protected[this] final def backoffOnError() {
    try { Thread.sleep(currentSleepTimeMs) } catch { case ie: InterruptedException => /* ignore interrupted exceptions */ }

    currentSleepTimeMs *= 2
    if (currentSleepTimeMs > backoffMaxTimeMs) {
      currentSleepTimeMs = backoffMaxTimeMs
    }
  }
}
