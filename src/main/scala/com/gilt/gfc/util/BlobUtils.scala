package com.gilt.gfc.util

import org.apache.commons.codec.binary.Base64

/** Prepares an object to be sent over the wire */
object BlobUtils {

  /** Makes a blob out of an object of type T.
    *
    * @param obj the object to blobify.
    * @return a base 64 encoded and zipped string representation of the obj.
    */ 
  def blob[T](obj: T): String = Base64.encodeBase64String(ZipUtils.zip[T](obj))

  /** Builds an obj of type T from a blob.
    *
    * @param s the base 64 encoded and zipped blob.
    * @return an object of type T.
    */ 
  def unblob[T](s: String): T = ZipUtils.unzip[T](Base64.decodeBase64(s))

}
