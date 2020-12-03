package aiven.kafkapg

import com.typesafe.config.ConfigFactory
import io.github.azhur.kafkaserdejson4s.Json4sSupport._
import monix.kafka.config.ObservableCommitOrder.AfterAck
import monix.kafka.{KafkaConsumerConfig, KafkaConsumerObservable}
import monix.reactive.Observable
import org.json4s._

import java.io.File

object KafkaConsumer {
  lazy val defaultConfig: KafkaConsumerConfig =
    KafkaConsumerConfig(ConfigFactory.parseFileAnySyntax(new File(".kafka/client.properties")))

  def consume[T >: Null <: AnyRef: Manifest]( topic:String, groupId:String, config:KafkaConsumerConfig = defaultConfig )
                                            ( implicit fmt: Formats ): Observable[T] = {
    implicit val ser: Serialization = org.json4s.jackson.Serialization
    KafkaConsumerObservable[String,T](config.copy(groupId = groupId, observableCommitOrder = AfterAck), List(topic))
      .map(_.value())
  }

}
