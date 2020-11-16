package aiven.kafkapg

import java.net.InetAddress
import java.time.Instant

import org.json4s._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import scala.io.Source
import scala.util.Using


class OsMetricsSpec extends AnyFunSpec with Matchers {
  def pingGoogle(): Boolean =  InetAddress.getByName("google.com").isReachable(100)

  describe("OsMetrics") {
    it("reads system metrics") {
      val s = OsMetrics.initial
      pingGoogle()
      val m = s.next
      m.cpuLoad.get should be >= 0d //TODO: >0 fails in WSL
      m.cpuLoad.get should be < 1d
      m.topCPUProcess.get should not be empty
      m.topMemProcess.get should not be empty
      m.freeMemBytes.get should be > 1000000L
      m.netInBytesPerS.get should be > 0L
      m.netOutBytesPerS.get should be > 0L
    }

    it("serializes with json4s") {
      implicit val fmt: Formats = Codecs.formats + Codecs.withSchema[OsMetrics]
      import AutoOption._
      val jv = Extraction.decompose(OsMetrics( Instant.EPOCH, 0.9D, 512, "snake", "xonix", 212, 4, "PDP-11" ))
      val schema = jv \ "schema"
      schema \ "type" should be(JString("struct"))

      def fieldByName(name:String) = ( for { JArray(fields) <- schema \ "fields"
                                             field <- fields if field \ "field" == JString(name)
                                           } yield field ).head
      fieldByName("hostName")        \ "type" should be(JString("string"))
      fieldByName("hostName")        \ "optional" should be(JBool(false))
      fieldByName("timestamp")       \ "type" should be(JString("int64"))
      fieldByName("timestamp")       \ "name" should be(JString("org.apache.kafka.connect.data.Timestamp"))
      fieldByName("timestamp")       \ "optional" should be(JBool(false))
      fieldByName("cpuLoad")         \ "type" should be(JString("double"))
      fieldByName("cpuLoad")         \ "optional" should be(JBool(true))
      fieldByName("freeMemBytes")    \ "type" should be(JString("int64"))
      fieldByName("freeMemBytes")    \ "optional" should be(JBool(true))
      fieldByName("topCPUProcess")   \ "type" should be(JString("string"))
      fieldByName("topCPUProcess")   \ "optional" should be(JBool(true))

      val payload = jv \ "payload"
      payload \ "timestamp" should be(JLong(0))
      payload \ "hostName" should be(JString("PDP-11"))
      payload \ "cpuLoad" should be(JDouble(0.9))
      payload \ "freeMemBytes" should be(JInt(512))
      payload \ "topMemProcess" should be(JString("xonix"))
      payload \ "topCPUProcess" should be(JString("snake"))
    }
  }
}

