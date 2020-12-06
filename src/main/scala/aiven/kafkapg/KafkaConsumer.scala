package aiven.kafkapg

import com.typesafe.config.ConfigFactory
import monix.eval.Task
import monix.kafka.config.AutoOffsetReset.Earliest
import monix.kafka.config.ObservableSeekOnStart.Beginning
import monix.kafka.{CommittableMessage, KafkaConsumerConfig, KafkaConsumerObservable}
import monix.reactive.Observable
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.io.File
import scala.util.{Failure, Success, Try}

object KafkaConsumer {
  lazy val defaultConfig: KafkaConsumerConfig =
    KafkaConsumerConfig(ConfigFactory.parseFileAnySyntax(new File(".kafka/client.properties")))
                       .copy( autoOffsetReset = Earliest, observableSeekOnStart = Beginning )

  case class Message[+V](v: V, raw: CommittableMessage[String,String]){
    def wrapTry[T](t: Task[T]): Task[Message[Try[T]]] = t.materialize.map(copy(_))
    def mapTry[T](f: V => Task[T] ): Task[Message[Try[T]]] = wrapTry(f(v))
  }

  def commit[V](msg: Message[V]):Task[V] = {
    msg.raw.committableOffset.commitSync().map(_=>msg.v)
  }

  def fragile[V](src: Observable[Message[Try[V]]])(implicit fmt: Formats, ser: Serialization): Observable[Message[V]] = src.mapEval {
    case Message(Success(v), raw) => Task(Message(v,raw))
    case Message(Failure(e),_) => Task.raiseError(e)
  }

  def careless[V](src: Observable[Message[Try[V]]])(implicit fmt: Formats, ser: Serialization): Observable[Message[V]] = src.collect {
    case Message(Success(v), raw) => Message(v,raw)
  }

  def json[V:Manifest](topic:String, groupId:String, config:KafkaConsumerConfig = defaultConfig )
                      (implicit fmt: Formats, ser: Serialization): Observable[Message[Try[V]]] = {
    KafkaConsumerObservable.manualCommit[String, String](config.copy(groupId=groupId), List(topic)).map { msg =>
      Message(Try( parse(msg.record.value).extract[V] ),msg)
    }
  }

}
