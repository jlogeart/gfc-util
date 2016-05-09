package com.gilt.gfc.util

import org.scalatest.{Matchers, FunSuite}

class ThrowablesTest extends FunSuite with Matchers {
  test("Linear root cause") {
    val root = new RuntimeException
    val top = (1 to 3).foldLeft(root)((exc, _) => new RuntimeException(exc))
    Throwables.rootCause(top) should be theSameInstanceAs root
  }

  test("Circular root cause") {
    val root = new RuntimeException
    val otherRoot = new RuntimeException(root)
    root.initCause(otherRoot)
    val top1 = (1 to 3).foldLeft(otherRoot)((exc, _) => new RuntimeException(exc))
    Throwables.rootCause(top1) should be theSameInstanceAs root

    val top2 = (1 to 3).foldLeft(root)((exc, _) => new RuntimeException(exc))
    Throwables.rootCause(top2) should be theSameInstanceAs otherRoot
  }

  test("Nested messages") {
    val top = List("bar", "baz").foldLeft(new RuntimeException("foo"))((exc, msg) => new RuntimeException(msg, exc))
    Throwables.messages(top) shouldBe Seq("baz", "bar", "foo")
  }

  test("Circular messages") {
    val one = new RuntimeException("foo")
    val two = new RuntimeException("bar", one)
    val three = new RuntimeException("baz", two)
    one.initCause(three)
    Throwables.messages(one) shouldBe Seq("foo", "baz", "bar")
    Throwables.messages(two) shouldBe Seq("bar", "foo", "baz")
    Throwables.messages(three) shouldBe Seq("baz", "bar", "foo")
  }

  test("Null messages") {
    val one = new RuntimeException("foo")
    val two = new RuntimeException(null, one)
    val three = new RuntimeException("baz", two)
    Throwables.messages(three) shouldBe Seq("baz", "foo")
  }

  test("Default messages") {
    val one = new RuntimeException("foo")
    val two = new RuntimeException(one)
    val three = new RuntimeException("baz", two)
    Throwables.messages(three) shouldBe Seq("baz", "java.lang.RuntimeException: foo", "foo")
  }

  test("Deduped messages") {
    val one = new RuntimeException("foo")
    val two = new RuntimeException("bar", one)
    val three = new RuntimeException("foo", two)
    Throwables.messages(three) shouldBe Seq("foo", "bar")
  }
}
