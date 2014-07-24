package com.gilt.gfc.util

import org.scalatest.{Matchers, FunSuite}

class ThrowablesTest extends FunSuite with Matchers {
  test("Linear root cause") {
    val root = newException()
    val top = (1 to 3).foldLeft(root)((exc, _) => newException(Some(exc)))
    Throwables.rootCause(top) should be(root)
  }

  test("Circular root cause") {
    val root = newException()
    val otherRoot = newException(Some(root))
    root.setCause(otherRoot)
    val top1 = (1 to 3).foldLeft(otherRoot)((exc, _) => newException(Some(exc)))
    Throwables.rootCause(top1) should be(root)

    val top2 = (1 to 3).foldLeft(root)((exc, _) => newException(Some(exc)))
    Throwables.rootCause(top2) should be(otherRoot)
  }

  def newException(t: Option[Throwable] = None): TestException = {
    val exc = new TestException
    t.foreach(exc.setCause)
    exc
  }
}

class TestException extends RuntimeException() {
  private var cause: Throwable = null
  def setCause(cause: Throwable): Unit = this.cause = cause
  override def getCause = cause
}

