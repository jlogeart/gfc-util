package com.gilt.gfc.util

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

class RateLimiterTest extends FunSuite with Checkers {

  test("Basics") {
    val seconds = 2   // < 2 seconds is not so great for the 1hz limiter
    for (hz <- List(1, 10, 100, 200)) {
      val limiter = new RateLimiter(hz)
      var counter = 0
      val start = System.currentTimeMillis
      while (System.currentTimeMillis < start + seconds * 1000) {
        limiter.limit {
          counter += 1
        }
      }
      // This seems the only reliable test in hudson.
      // Was seeing very large differences like counter == 207, seconds*hz == 400
      // (Due to context switching?)
      assert(counter <= seconds * hz, "counter = %d, hz = %d".format(counter, seconds * hz))
    }
  }

  test("Unlimited mode") {
    val limiter = new RateLimiter(0)
    var counter = 0
    val start = System.currentTimeMillis
    while (System.currentTimeMillis < start + 1000) {
      limiter.limit {
        counter += 1
      }
    }
    assert(counter > 1000)  // surely we can count faster than once per ms
  }
}
