package aiven.kafkapg

import java.time.Instant

import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.json.JsonConverter
import org.apache.kafka.connect.storage.{ConverterConfig, ConverterType}
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, _}
import org.json4s.jackson.JsonMethods

import scala.jdk.CollectionConverters._


object KafkaConnectJson {
  private val schemaConv = new JsonConverter()
  schemaConv.configure(Map(ConverterConfig.TYPE_CONFIG -> ConverterType.VALUE.getName).asJava)

  trait HasSchema[T] { def schema: Schema }

  def withSchema[T: HasSchema]: Serializer[T] = new Serializer[T] {
    val schema: Schema = implicitly[HasSchema[T]].schema

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), T] = {
      case (_, json) => Extraction.extract(json)
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case v  => ("schema" -> JsonMethods.fromJsonNode(schemaConv.asJsonSchema(schema))) ~
                 ("payload" -> Extraction.decompose(v)(format - this))
    }
  }
}
