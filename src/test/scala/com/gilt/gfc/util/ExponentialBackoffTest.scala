package com.gilt.gfc.util

import java.util.concurrent.{TimeUnit, CountDownLatch}

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
        throw new RuntimeException("Boo!")
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

      numTries should be(3)
    }
  }

  test("backoff grows exponentially") {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      val timestamps = new scala.collection.mutable.ListBuffer[Long]()

      retry {
        timestamps += System.currentTimeMillis()
        if (timestamps.size < 12) sys.error("")
      }

      val lapses = timestamps.sliding(2).toSeq.map(t => t(1) - t(0))
      // drop the first 6 (1-32ms) as they may be erratic on slow machines
      lapses.drop(6).sliding(2).foreach { t =>
        t(1) should be > t(0)
        (t(0) * 2D) should be > (t(1) * 0.75D)
        (t(0) * 2D) should be < (t(1) * 1.25D)
      }
    }
  }

  test("backoff sleeps the min duration") {
    new ExponentialBackoff with Loggable {
      override val backoffMinTimeMs = 64L
      override val backoffMaxTimeMs = 150L

      val timestamps = new scala.collection.mutable.ListBuffer[Long]()

      retry {
        timestamps += System.currentTimeMillis()
        if (timestamps.size < 5) sys.error("")
      }

      val lapses = timestamps.sliding(2).toSeq.map(t => t(1) - t(0))
      lapses.size should be (4)
      lapses.foreach(_ should be >= 64L)
    }
  }

  test("backoff maxes out") {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 100L

      val timestamps = new scala.collection.mutable.ListBuffer[Long]()

      retry {
        timestamps += System.currentTimeMillis()
        if (timestamps.size < 10) sys.error("")
      }

      val lapses = timestamps.sliding(2).toSeq.map(t => t(1) - t(0))
      lapses.size should be (9)
      lapses.foreach(_ should be < 150L)
    }
  }

  test("backoff is side effect free") {
    new ExponentialBackoff with Loggable {
      override val backoffMaxTimeMs = 10 * 1000L

      val latch = new CountDownLatch(1)
      var numTries = 0

      val t = new Thread(new Runnable {
        override def run {
          retry {
            numTries += 1
            if (numTries < 8) sys.error("Boo!")
            latch.countDown()
            Thread.sleep(backoffMaxTimeMs)
          }
        }
      })
      t.setDaemon(true)
      t.start()

      latch.await(500L, TimeUnit.MILLISECONDS) should be(true)

      val now = System.currentTimeMillis()
      numTries = 0
      retry {
        numTries += 1
        if (numTries < 2) sys.error("Boo!")
      }

      System.currentTimeMillis() should be < (now + 100)
    }
  }
}
