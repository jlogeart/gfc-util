package com.gilt.gfc.util

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{Matchers, FunSuite}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration._

class RetryTest extends FunSuite with Matchers {
  def succeedF: String = "yay"
  def failF1: String = throw new RuntimeException("boom")
  def failF2: String = throw new RuntimeException("crash")

  test("retry should retry function until it succeeds") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF1 _, failF1 _, succeedF _)

    Retry.retry()(functions.next.apply) shouldBe "yay"
  }

  test("retry should retry until maxRetries") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF2 _, succeedF _)

    val thrown = the [Exception] thrownBy Retry.retry(1)(functions.next.apply)
    thrown.getMessage shouldBe "crash"
  }

  test("retryWithExponentialDelay should retry function until it succeeds") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF1 _, failF1 _, succeedF _)

    Retry.retryWithExponentialDelay()(functions.next.apply) shouldBe "yay"
  }

  test("retryWithExponentialDelay should retry until maxRetries") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF2 _, succeedF _)

    val thrown = the [Exception] thrownBy Retry.retryWithExponentialDelay(maxRetryTimes = 1)(functions.next.apply)
    thrown.getMessage shouldBe "crash"
  }

  test("retryWithExponentialDelay should retry until maxRetryTimeout") {
    val functions: Iterator[() => String] = Iterator.continually(failF1 _)

    val start = System.currentTimeMillis()
    val thrown = the [RuntimeException] thrownBy Retry.retryWithExponentialDelay(maxRetryTimeout = 100 millis fromNow)(functions.next.apply)
    thrown.getMessage shouldBe "boom"
    (System.currentTimeMillis() - start) should be (120L +- 20L)
  }


  test("retryWithExponentialDelay should apply exponential backoff") {
    val times = new VectorBuilder[Long]

    val counter = new AtomicInteger(0)
    def func: String = {
      val c = counter.incrementAndGet()
      times += System.currentTimeMillis()
      if (c >= 7) {
        "ok"
      } else {
        throw new Exception(s"error $c")
      }
    }

    times += System.currentTimeMillis()

    // Delay series should be (ms): 100, 150, 225, 337, 500, 500
    Retry.retryWithExponentialDelay(initialDelay = 100 millis,
                                    maxDelay = 500 millis,
                                    exponentFactor = 1.5)(func) shouldBe "ok"

    val timeDeltas = times.result().sliding(2).map(v => v(1) - v(0)).toList
    timeDeltas.length shouldBe 7
    timeDeltas(0) shouldBe 20L  +- 20L
    timeDeltas(1) shouldBe 120L +- 20L
    timeDeltas(2) shouldBe 170L +- 20L
    timeDeltas(3) shouldBe 245L +- 20L
    timeDeltas(4) shouldBe 357L +- 20L
    timeDeltas(5) shouldBe 520L +- 20L
    timeDeltas(6) shouldBe 520L +- 20L
  }

}
