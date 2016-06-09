package com.gilt.gfc.util

import java.io.InputStream
import java.net.URL
import java.util.jar.Manifest
import scala.util.control.NonFatal

/**
 * Used to get hold of the version of a particular item in the manifest.
 */
trait VersionUtil {

  /**
   * This trawls through all the jar files, so just call this once per JVM.
   *
   * @param title value of Implementation-Title key to filter from all the manifests on the classpath
   * @return version of a given library or app based on the gilt MANIFEST.MF Implementation-Version standard
   */
  def loadVersion(title: String): Option[String] = {
    import scala.collection.JavaConverters._

    this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF").asScala.map(readManifest).toSeq.
      flatten.find(_.getMainAttributes.getValue("Implementation-Title") == title).
      flatMap(m => Option(m.getMainAttributes.getValue("Implementation-Version")))
  }

  def readManifest(url: URL): Option[Manifest] = {
    safeReadUrl(url)(new Manifest(_))
  }

  def safeReadUrl[T](url: URL)(f: InputStream => T): Option[T] = {
    try {
      val input = url.openStream()
      try {
        Some(f(input))
      } finally {
        try { input.close() } catch { case NonFatal(_) => /* ignore */ }
      }
    } catch {
      case e: Exception => None
    }
  }
}

object VersionUtil extends VersionUtil
