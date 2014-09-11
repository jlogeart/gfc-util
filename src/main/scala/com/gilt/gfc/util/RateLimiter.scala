package com.gilt.gfc.util

import scala.beans.BeanProperty
import com.gilt.gfc.collection.CircularBuffer

/**
 * Used to rate-limit calls to a work function, e.g. that writes to a db.
 *
 * Note you can disable the rate limiter by setting the frequency to 0.
 *
 * The math is as follows (mod some conversions between seconds and nano-seconds):
 *
 * After n samples, we can computer Ri as the "immediate rate" like:
 *
 *    Ri = (n-1) / (timestamp(n) - timestamp(0))
 *
 * which is essentially the 1/average interval.
 *
 * Given a target rate, Rt ("maxFreqHz"), we can compute it like:
 *
 *    Rt = n / (S + (timestamp(n) - timestamp(0))
 *
 * S here is how long to sleep before we invoke the next operation.  Solving for S:
 *
 *    S = n / Rt - (timestamp(n) - timestamp(0))
 *
 * @param maxFreqHz Frequency the system should strive to achieve on average. Not a guarantee
 */
class RateLimiter(@BeanProperty val maxFreqHz: Int) {
  require(maxFreqHz >= 0, "Frequency < 0 behavior not defined")
  private val buffer = new CircularBuffer[Long](math.max(3, maxFreqHz))    // small freq needs some buffer
  private val maxFreqNanoHz = maxFreqHz / 1000000000d                         // convert 1/s to 1/nanoseconds

  def limit[T](workFunction: =>T): T = {
    if (maxFreqHz == 0) {
      workFunction
    } else {
      buffer.add(System.nanoTime)     // record the timestamp first! otherwise delays between invocations don't count
      if (buffer.size > 1) {          // at least 2 samples required to be able to define this
        val sum = buffer.newest - buffer.oldest     // Sum of all intervals
        val sleepTime = (buffer.size / maxFreqNanoHz) - sum
        val sleepMs = math.round((sleepTime)/1000000)   // how long sleep to achieve maxFreqHz
        val sleepNs = math.round((sleepTime)%1000000).toInt
        if (sleepMs > 0 || sleepNs > 0) {
          Thread.sleep(sleepMs, sleepNs)
        }
      }
      workFunction    // do what we are meant to do and return result
    }
  }
}

/**
 * Fairly heavy-handed thread safe version of the above. There's not a lot of point to trying to avoid
 * locking here, since the whole point of this thing is to block writes, so we might as well keep it simple.
 *
 * @param maxFreqHz Frequency the system should strive to achieve on average. Not a guarantee
 */
class ThreadSafeRateLimiter(maxFreqHz: Int) extends RateLimiter(maxFreqHz) {
  override def limit[T](workFunction: =>T): T = {
    synchronized(super.limit(workFunction))
  }
}

/**
 * Used by JRateLimiter in lieu of call-by-name from java.
 */
trait LimitCallback[T <: AnyRef] {
  def apply: T
}

/**
 * Java-friendly version of ThreadSafeRateLimiter.
 */
class JRateLimiter(maxFreqHz: Int) extends ThreadSafeRateLimiter(maxFreqHz) {
  def rateLimit[T <: AnyRef](callback: LimitCallback[T]): T = {
    super.limit(callback.apply)
  }
}
