package aiven.kafkapg

import java.time.Instant

import org.json4s._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._


class OsMetricsSpec extends AnyFunSpec with Matchers {

  describe("serializer") {
    it("serializes with json4s") {
      implicit val fmt: Formats = Codecs.formats + Codecs.withSchema[OsMetrics]
      val jv = Extraction.decompose(OsMetrics(Instant.EPOCH, "PDP-11", -1))
      val schema = jv \ "schema"
      schema \ "type" should be(JString("struct"))
      val fields = schema \ "fields"
      fields(0) \ "type" should be(JString("string"))
      fields(0) \ "field" should be(JString("hostName"))
      fields(1) \ "type" should be(JString("double"))
      fields(1) \ "field" should be(JString("cpuLoad"))
      fields(2) \ "name" should be(JString("org.apache.kafka.connect.data.Timestamp"))
      fields(2) \ "field" should be(JString("timestamp"))
      fields(2) \ "version" should be(JInt(1))
      val payload = jv \ "payload"
      payload \ "timestamp" should be(JLong(0))
      payload \ "hostName" should be(JString("PDP-11"))
      payload \ "cpuLoad" should be(JDouble(-1))

      //Expected JSON:
      /* {
        "schema" : {
          "type" : "struct",
          "fields" : [ { "type" : "string",
            "optional" : false,
            "field" : "hostName"
          }, {
            "type" : "double",
            "optional" : false,
            "field" : "cpuLoad"
          }, {
            "type" : "int64",
            "optional" : false,
            "name" : "org.apache.kafka.connect.data.Timestamp",
            "version" : 1,
            "field" : "timestamp"
          } ],
          "optional" : false
        },
        "payload" : {
          "schema" : {
            "type" : "struct",
            "fields" : [ {
              "type" : "string",
              "optional" : false,
              "field" : "host"
            }, {
              "type" : "double",
              "optional" : false,
              "field" : "cpuLoad"
            }, {
              "type" : "int64",
              "optional" : false,
              "name" : "org.apache.kafka.connect.data.Timestamp",
              "version" : 1,
              "field" : "timeStamp"
            } ],
            "optional" : false
          },
          "payload" : {
            "timestamp" : 0,
            "hostName" : "PDP-11",
            "cpuLoad" : -1.0
          }
        }
      }
       */

    }
  }
}

