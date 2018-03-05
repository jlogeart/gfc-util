package com.gilt.gfc.util

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{Matchers, FunSuite}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration._

class RetryTest extends FunSuite with Matchers {
  implicit def logSuppressor(t: Throwable): Unit = {}
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

  test("retry should retry code throwing InterruptedException") {
    val functions: Iterator[() => String] = Iterator(() => throw new InterruptedException("interrupt"), succeedF _)
    Retry.retry(maxRetryTimes = 1)(functions.next.apply) shouldBe "yay"
  }

  test("retry should re-throw an Error") {
    val functions: Iterator[() => String] = Iterator(() => throw new OutOfMemoryError("oom"), succeedF _)
    val thrown = the [OutOfMemoryError] thrownBy Retry.retry(maxRetryTimes = 1)(functions.next.apply)
    thrown.getMessage shouldBe "oom"
  }

  test("retryWithExponentialDelay should retry function until it succeeds") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF1 _, failF1 _, succeedF _)

    Retry.retryWithExponentialDelay()(functions.next.apply) shouldBe "yay"
  }

  test("retryWithExponentialDelay should retry until maxRetryTimes") {
    val functions: Iterator[() => String] = Iterator(failF1 _, failF2 _, succeedF _)

    val thrown = the [Exception] thrownBy Retry.retryWithExponentialDelay(maxRetryTimes = 1)(functions.next.apply)
    thrown.getMessage shouldBe "crash"
  }

  test("retryWithExponentialDelay should retry until maxRetryTimeout") {
    var count = 0
    def function: String = {
      count += 1
      failF1
    }

    val start = System.currentTimeMillis()
    val thrown = the [RuntimeException] thrownBy Retry.retryWithExponentialDelay(maxRetryTimeout = 100 millis fromNow)(function)
    thrown.getMessage shouldBe "boom"
    (System.currentTimeMillis() - start) should be (120L +- 20L)
    count shouldBe (8 +- 1)
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

  test("retryWithExponentialDelay should retry code throwing InterruptedException") {
    val functions: Iterator[() => String] = Iterator(() => throw new InterruptedException("interrupt"), succeedF _)
    Retry.retryWithExponentialDelay(maxRetryTimes = 1)(functions.next.apply) shouldBe "yay"
  }

  test("retryWithExponentialDelay should re-throw an Error") {
    val functions: Iterator[() => String] = Iterator(() => throw new OutOfMemoryError("oom"), succeedF _)
    val thrown = the [OutOfMemoryError] thrownBy Retry.retryWithExponentialDelay(maxRetryTimes = 1)(functions.next.apply)
    thrown.getMessage shouldBe "oom"
  }

  test("retryFold should retry function until it succeeds") {
    Retry.retryFold()(0)(i =>
      if (i == 10) Right("yay")
      else Left(i + 1)
    ) shouldBe "yay"
  }

  test("retryFold should retry until maxRetries") {
    val thrown = the [Retry.TooManyRetries[Int]] thrownBy Retry.retryFold(1)(0)(i => Left(i+1))
    thrown.lastInput shouldBe 2
    thrown.wrapped shouldBe None
  }

  test("retryFold should wrap the underlying Exception") {
    val ex = new RuntimeException("boom")
    val thrown = the [Retry.TooManyRetries[Int]] thrownBy Retry.retryFold(1)(0)(i => throw ex)
    thrown.lastInput shouldBe 0
    thrown.wrapped shouldBe Some(ex)
  }

  test("retryFoldWithExponentialDelay should retry function until it succeeds") {
    Retry.retryFoldWithExponentialDelay()(0)(i =>
      if (i == 10) Right("yay")
      else Left(i + 1)
    ) shouldBe "yay"
  }

  test("retryFoldWithExponentialDelay should retry until maxRetries") {
    val thrown = the [Retry.TooManyRetries[Int]] thrownBy Retry.retryFoldWithExponentialDelay(maxRetryTimes = 1)(0)(i => Left(i+1))
    thrown.lastInput shouldBe 2
    thrown.wrapped shouldBe None
  }

  test("retryFoldWithExponentialDelay should wrap the underlying Exception") {
    val ex = new RuntimeException("boom")
    val thrown = the [Retry.TooManyRetries[Int]] thrownBy Retry.retryFoldWithExponentialDelay(maxRetryTimes = 1)(0)(i => throw ex)
    thrown.lastInput shouldBe 0
    thrown.wrapped shouldBe Some(ex)
  }
}
