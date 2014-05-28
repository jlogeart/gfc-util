package com.gilt.gfc.util

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

/**
 * For objects that need their lifecycle to be managed and can't be just throw-away.
 * E.g. for when ConcurrentHashMap.putIfAbsent(new Something()) may result in a Something
 * instance that need to be closed if another thread's putIfAbsent was successful.
 *
 * In this case it's easier to not create unnecessary instances in the first place.
 */
class SingletonCache[K] {
  private[this] val instanceCache = new ConcurrentHashMap[K, CachedValue[_]]().asScala

  /**
   * Get a cached value for a key or generate a new one if missing.
   * Only calls generator() once for each missing key.
   * Generator is given here and not in the constructor to allow for caching of values of different types
   * where type info is required for generator to work. E.g. calls that use reflection.
   */
  def apply[V](key: K)(generator: => V): V = {
    instanceCache.get(key).getOrElse {
      instanceCache.putIfAbsent(key, new CachedValue[V](() => generator))
      instanceCache(key)
    }.instance.asInstanceOf[V]
  }

  def values[V]: Iterable[V] = instanceCache.values.map(_.instance.asInstanceOf[V])

  // scala has a nice 'lazy' implementation that is both efficient and safe,
  // based on oft-criticized double-checked lock pattern except that it's actually done correctly:)
  private[this] class CachedValue[V](var generator: () => V) {
    lazy val instance: V = { // generator will be only called once due to lazy
      val once = generator()
      generator = null // CachedValue is long-lived, this makes generator eligible for GC
      once
    }
  }
}
