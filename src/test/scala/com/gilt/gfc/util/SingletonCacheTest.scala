package com.gilt.gfc.util

import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.{Matchers, FunSuite}

class SingletonCacheTest extends FunSuite with Matchers {
  test("SingletonCache") {
    def makeCounters = new scala.collection.mutable.HashMap[String, AtomicInteger]() {
      override def default(k: String): AtomicInteger = {
        this += k -> new AtomicInteger
        this(k)
      }
    }

    val cache = new SingletonCache[String]()
    val numCalls = makeCounters

    def mkObj(key: String) = cache(key) {
      numCalls(key).incrementAndGet
      new Object
    }

    val foo1 = mkObj("foo")
    foo1 should be theSameInstanceAs(mkObj("foo"))
    foo1 should be theSameInstanceAs(mkObj("foo"))

    val bar1 = mkObj("bar")
    bar1 should be theSameInstanceAs(mkObj("bar"))
    bar1 should be theSameInstanceAs(mkObj("bar"))

    // the whole point of this object is to only call given closure once
    // for each distinct key
    numCalls.values.map(_.get).toList should be(List(1,1))
  }
}
