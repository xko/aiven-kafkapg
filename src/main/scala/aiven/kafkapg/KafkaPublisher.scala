package aiven.kafkapg

import java.io.File

import com.typesafe.config.ConfigFactory
import monix.execution.Scheduler
import monix.kafka.{KafkaProducerConfig, KafkaProducerSink}
import monix.reactive.Observable
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Await
import scala.concurrent.duration._
import io.github.azhur.kafkaserdejson4s.Json4sSupport
import org.json4s._


object KafkaPublisher extends App with Json4sSupport {
  implicit val fmt: Formats = Codecs.formats + Codecs.withSchema[OsMetrics]
  implicit val ser: Serialization = org.json4s.jackson.Serialization

  val metrics = Observable.interval(3.second).scan(OsMetrics.initial)( (metrics, _) => metrics.next )
  val records = metrics.map(
    new ProducerRecord[String, OsMetrics]("os_metrics", null, _)
  )

  val producerCfg = KafkaProducerConfig(ConfigFactory.parseFileAnySyntax(new File("kafka.client.properties")))
  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  val producer = KafkaProducerSink[String,OsMetrics](producerCfg, scheduler)
  val doit = records.bufferIntrospective(1024).consumeWith(producer).runToFuture
  Await.result(doit, Duration.Inf)

}
