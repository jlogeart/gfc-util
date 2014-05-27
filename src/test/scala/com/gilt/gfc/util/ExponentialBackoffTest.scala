package com.gilt.gfc.util

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class ExponentialBackoffTest extends TestNGSuite with ShouldMatchers {
  @Test
  def testLoopWithBackoffOnErrorWhile() {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      val startTime = System.currentTimeMillis
      var numTries = 0
      loopWithBackoffOnErrorWhile(numTries < 10) {
        numTries += 1
        sys.error("Boo!")
      }
      val endTime = System.currentTimeMillis
      numTries should be(10)
      (endTime - startTime) should be > (1000L)
    }
  }

  @Test
  def testRetryOnErrorEventuallySucceeds() {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      var numTries = 0
      val myResult = retry {
        numTries += 1
        if (numTries < 3) sys.error("Boo!")
        "expected result"
      }

      numTries should be(3)
      myResult should be("expected result")
    }
  }

  @Test
  def testRetryUpToExitsAfterMaxRetry() {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      var numTries = 0

      val thrown = evaluating {
        retryUpTo(3) {
          numTries += 1
          sys.error("Boo!")
        }
      } should produce[Exception]

      thrown.getMessage should startWith("Max number of retries")

      numTries should be(3)
    }
  }
}
