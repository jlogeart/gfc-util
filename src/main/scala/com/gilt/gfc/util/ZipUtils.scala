package com.gilt.gfc.util

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

/** Util to zip/unzip an object/binary blob in/to a binary blob/object.
  *
  * Actual impl of the compression is plain GZIP classes from the Java JDK.
  */
object ZipUtils {

  /** Compress a generic object.
   * 
    * @param stuff the object to be compressed (of type T).
    * @return an array of bytes with the compressed binary blob. 
    */ 
  def zip[T](stuff: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream 
    val ozbos = new ObjectOutputStream(new GZIPOutputStream(bos))

    try {
      ozbos.writeObject(stuff)
    } finally {
      ozbos.close
    }

    bos.toByteArray
  }

  /** Uncompress a given array of bytes. 
    * 
    * @param bytes the array of bytes containing the zipped blob.
    * @return an instance of the T type.
    */ 
  def unzip[T](bytes: Array[Byte]): T = {
    val bis = new ByteArrayInputStream(bytes)
    val zbis = new GZIPInputStream(bis)
    val ozbis = new ObjectInputStream(zbis)
  
    try {
      ozbis.readObject.asInstanceOf[T]
    } finally {
      ozbis.close
    }
  } 
}
