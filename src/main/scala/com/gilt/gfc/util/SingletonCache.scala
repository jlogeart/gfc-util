package com.gilt.gfc.util

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.collection.Map

/**
 * For objects that need their lifecycle to be managed and can't be just throw-away.
 * E.g. for when ConcurrentHashMap.putIfAbsent(new Something()) may result in a Something
 * instance that need to be closed if another thread's putIfAbsent was successful.
 *
 * In this case it's easier to not create unnecessary instances in the first place.
 */
class SingletonCache[K] {
  private[this] val instanceCache = new ConcurrentHashMap[K, Any]()

  /**
    * Get a cached value for a key or generate a new one if missing.
    * Only calls generator() once for each missing key.
    * Generator is given here and not in the constructor to allow for caching of values of different types
    * where type info is required for generator to work. E.g. calls that use reflection.
    */
  def apply[V](key: K)(generator: => V): V =
    instanceCache.computeIfAbsent(key, _ => generator).asInstanceOf[V]

  def values[V]: Iterable[V] = instanceCache.values.asScala.map(_.asInstanceOf[V])

  def asMap[V]: Map[K, V] = instanceCache.asScala.mapValues(_.asInstanceOf[V])

}
