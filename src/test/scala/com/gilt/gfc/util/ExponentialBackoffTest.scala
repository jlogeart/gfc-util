package com.gilt.gfc.util

import com.gilt.gfc.logging.Loggable
import org.scalatest.{FunSuite, Matchers}

class ExponentialBackoffTest extends FunSuite with Matchers {
  test("loopWithBackoffOnErrorWhile") {
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

  test("Retry on error eventually succeeds") {
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

  test("Retry up to exits after max retry") {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      var numTries = 0

      val thrown = the [Exception] thrownBy {
        retryUpTo(3) {
          numTries += 1
          sys.error("Boo!")
        }
      }

      thrown.getMessage should startWith("Max number of retries")

      numTries should be(3)
    }
  }
}
