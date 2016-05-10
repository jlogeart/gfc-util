package com.gilt.gfc.util

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.scalatest.{Matchers, FunSuite}

class ThrowablesTest extends FunSuite with Matchers {
  test("null throws NPE") {
    a[NullPointerException] shouldBe thrownBy(Throwables.rootCause(null))
  }

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

  test("Is a RuntimeException") {
    val rte = new RuntimeException("foo", new Exception)
    val isAnRTE = Throwables.isA(classOf[RuntimeException]) _

    isAnRTE(rte) shouldBe Some(rte)
    isAnRTE(new Exception(rte)) shouldBe Some(rte)
    isAnRTE(new Exception(new Exception)) shouldBe None

    (new Exception("bang", rte) match {
      case ex if isAnRTE(ex).isDefined => ex.getMessage
    }) shouldBe "bang"
  }

  test("pattern matching nested exceptions") {
    val nestedRte = new Exception(new RuntimeException("boom"))
    val noRte = new Exception(new Exception())

    {
      try(throw nestedRte)
      catch Throwables {
        case rte: RuntimeException => rte.getMessage
      }
    } shouldBe "boom"

    an[Exception] shouldBe thrownBy {
      try(throw noRte)
      catch Throwables {
        case rte: RuntimeException => rte.getMessage
      }
    }

    Throwables {
      case rte: RuntimeException => rte.getMessage
    }{
      nestedRte
    } shouldBe "boom"

    an[Exception] shouldBe thrownBy {
      Throwables {
        case rte: RuntimeException => rte.getMessage
      }(noRte)
    }

    Try(throw nestedRte).recover(Throwables {
      case rte: RuntimeException => rte.getMessage
    }) shouldBe Success("boom")

    Try(throw noRte).recover(Throwables {
      case rte: RuntimeException => rte.getMessage
    }) shouldBe Failure(noRte)
  }

  test("extracting nested exceptions") {
    val nestedRte = new Exception(new RuntimeException("boom"))
    val noRte = new Exception(new Exception())

    val RTE = Throwables.Matcher(classOf[RuntimeException])

    {
      nestedRte match {
        case RTE(e) => e.getMessage
      }
    } shouldBe "boom"

    {
      nestedRte match {
        case RTE(e) if e.getMessage == "boom" => e.getMessage
      }
    } shouldBe "boom"

    an[Exception] shouldBe thrownBy {
      nestedRte match {
        case RTE(e) if e.getMessage == "bang" => e.getMessage
      }
    }

    an[Exception] shouldBe thrownBy {
      noRte match {
        case RTE(e) => e.getMessage
      }
    }
  }
}
