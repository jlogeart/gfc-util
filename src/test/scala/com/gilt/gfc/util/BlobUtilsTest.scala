package com.gilt.gfc.util

import java.util.UUID

import org.scalatest.{FunSuite, Matchers}

class BlobUtilsTest extends FunSuite with Matchers {

  private val uuids = for (i <- 1 until 23) yield { java.util.UUID.randomUUID } 
  private val longs = new Array[Long](uuids.size * 2)

  for (i <- 0 until uuids.size) {
    longs(i * 2) = uuids(i).getMostSignificantBits
    longs(i * 2 + 1) = uuids(i).getLeastSignificantBits
  }
  
  test("Blob of sequence of UUIDs works OK") {
    val actual = BlobUtils.unblob[Seq[UUID]](BlobUtils.blob[Seq[UUID]](uuids))
    assert(uuids === actual)
  }

  test("Blob of array of Longs works OK") {
    val actual = BlobUtils.unblob[Array[Long]](BlobUtils.blob[Array[Long]](longs))
    assert(longs === actual)
  }

  test("Blob of empty sequence works OK") {
    val actual = BlobUtils.unblob[Seq[Int]](BlobUtils.blob[Seq[Int]](Seq.empty[Int]))
    assert(Seq.empty[Int] === actual)
  }
}
