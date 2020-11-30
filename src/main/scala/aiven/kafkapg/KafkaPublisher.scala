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

  val defaultTopic = "os_metrics"

  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  def publish[T <:AnyRef : Schematic](items: Observable[T], topic:String = defaultTopic,
                                      config: KafkaProducerConfig = defaultConfig): Task[Unit] = {
    implicit val fmt: Formats = Codecs.formats + Codecs.withSchema[T]
    implicit val ser: Serialization = org.json4s.jackson.Serialization

    val records = items.map( new ProducerRecord[String, T](topic, null, _) )
    val producer = KafkaProducerSink[String,T](config, scheduler)
    records.bufferIntrospective(1024).consumeWith(producer)
  }

  def main(args: Array[String]): Unit = {
    val go = publish(Observable.interval(3.second).scan(OsMetrics.initial)((metrics, _) => metrics.next))
             .runToFuture
    Await.ready(go, Duration.Inf).value.get.get
  }

}
