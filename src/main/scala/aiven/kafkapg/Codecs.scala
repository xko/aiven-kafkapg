package aiven.kafkapg

import java.time.Instant

import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.json.JsonConverter
import org.apache.kafka.connect.storage.{ConverterConfig, ConverterType}
import org.json4s
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, _}
import org.json4s.jackson.JsonMethods

import scala.jdk.CollectionConverters._

trait Schematic[T] { def schema: Schema }

object Codecs {
  val jc = new JsonConverter()
  jc.configure( Map(ConverterConfig.TYPE_CONFIG -> ConverterType.VALUE.getName).asJava )

  val InstantAsLong = new CustomSerializer[Instant]( _ => (
    { case ms: JLong => Instant.ofEpochMilli(ms.values)},
    { case ms: Instant => JLong(ms.toEpochMilli) }
  ))

  def withSchema[T: Schematic]: Serializer[T] = new Serializer[T] {
    val schematic: Schematic[T] = implicitly

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), T] = {
      case (_, json) => Extraction.extract(json)
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case v  => ("schema" -> JsonMethods.fromJsonNode(jc.asJsonSchema(schematic.schema))) ~
                 ("payload" -> Extraction.decompose(v)(formats - this))
    }
  }

  val formats: Formats = DefaultFormats + InstantAsLong
}
