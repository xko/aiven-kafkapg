package aiven.kafkapg

import com.typesafe.config.ConfigFactory
import io.github.azhur.kafkaserdejson4s.Json4sSupport
import monix.eval.Task
import monix.execution.Scheduler
import monix.kafka.{KafkaProducerConfig, KafkaProducerSink}
import monix.reactive.Observable
import org.apache.kafka.clients.producer.ProducerRecord
import org.json4s._

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._


object KafkaPublisher extends Json4sSupport {
  lazy val defaultConfig: KafkaProducerConfig =
    KafkaProducerConfig( ConfigFactory.parseFileAnySyntax(new File(".kafka/client.properties")) )

  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  def publish[T <:AnyRef]( items: Observable[T], topic:String, config: KafkaProducerConfig = defaultConfig )
                         ( implicit fmt: Formats ): Task[Unit] = {
    implicit val ser: Serialization = org.json4s.jackson.Serialization

    val records = items.map( new ProducerRecord[String, T](topic, null, _) )
    val producer = KafkaProducerSink[String,T](config, scheduler)
    records.bufferIntrospective(1024).consumeWith(producer)
  }

  def publish4KConnect[T <:AnyRef : KafkaConnectJson.HasSchema](items: Observable[T], topic:String,
                                                                config: KafkaProducerConfig = defaultConfig ): Task[Unit] =
    publish(items, topic, config)(KafkaConnectJson.formats + KafkaConnectJson.withSchema[T])

}
