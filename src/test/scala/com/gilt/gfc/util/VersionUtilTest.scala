package com.gilt.gfc.util

import java.io.{FileOutputStream, File}
import java.net.URL
import java.util.jar.{Attributes, Manifest => JManifest}
import org.scalatest.FunSuite
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

class VersionUtilTest extends FunSuite with MockitoSugar {

  test("Happy Flow") {
    val util = new VersionUtil {
      override def readManifest(url: URL): Option[JManifest] = {
        val mf: JManifest = mockOf[JManifest] { manifest =>
            when(manifest.getMainAttributes).thenReturn {
              val attr = new Attributes
              attr.put(new Attributes.Name("Implementation-Title"), "Syndication Service")
              attr.put(new Attributes.Name("Implementation-Version"), "3.14")
              attr
            }
        }
        Some(mf)
      }
    }

    assert(util.loadVersion("Syndication Service") === Some("3.14"))
    assert(util.loadVersion("FOO") === None)
  }

  test("Read Manifest") {
    val mf = new JManifest()
    mf.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    mf.getMainAttributes.putValue("Implementation-Title", "Foo")
    mf.getMainAttributes.putValue("Implementation-Version", "Bar")

    val tmpFile = File.createTempFile("testReadManifest", ".tmp")
    tmpFile.deleteOnExit()

    val os = new FileOutputStream(tmpFile)
    mf.write(os)
    os.close

    val option = VersionUtil.readManifest(tmpFile.toURI.toURL)
    assert(option.isDefined)
    assert(option.get.getMainAttributes.getValue("Implementation-Title") === "Foo")
    assert(option.get.getMainAttributes.getValue("Implementation-Version") === "Bar")
  }

  test("Open Stream Fails") {
    assert(VersionUtil.readManifest(new URL("http://localhost:1")) === None)
  }

  test("Construct From Stream Fails") {
    val tmpFile = File.createTempFile("testConstructFromStreamFails", ".tmp")
    tmpFile.deleteOnExit()
    assert(VersionUtil.safeReadUrl(tmpFile.toURI.toURL)(_ => throw new Exception) === None)
  }

  private def mockOf[T <: AnyRef](f: T => Unit)(implicit mf: Manifest[T]): T = {
    val mck = mock[T]
    f(mck)
    mck
  }
}
