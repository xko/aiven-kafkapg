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

  case class Message[+V](value: V, raw: CommittableMessage[String,String]){
    def to[T](v:T): Message[T] = copy(value = v)
  }

  def commit[V](msg: Message[V]):Task[V] = {
    msg.raw.committableOffset.commitSync().map(_=>msg.value)
  }

  def fragile[V](src: Observable[Message[Task[V]]]): Observable[Message[V]] = src.mapEval  { m =>
    m.value.materialize.map(d => m.to(d))
  } mapEval {
    case Message(Success(v), raw) => Task(Message(v,raw))
    case Message(Failure(e),_)    => Task.raiseError(e)
  }

  def careless[V](src: Observable[Message[Task[V]]]): Observable[Message[V]] = src.mapEval { m =>
    m.value.materialize.map(d => m.to(d))
  }.collect {
    case Message(Success(v), raw) => Message(v,raw)
  }

  def insistent[V](src: Observable[Message[Task[V]]]): Observable[Message[V]] = src.mapEval { m =>
    m.value.onErrorRestart(10).map(m.to)
  }

  def json[V:Manifest](topic:String, groupId:String, config:KafkaConsumerConfig = defaultConfig )
                      (implicit fmt: Formats, ser: Serialization): Observable[Message[Task[V]]] = {
    KafkaConsumerObservable.manualCommit[String, String](config.copy(groupId=groupId), List(topic)).map { msg =>
      Message(Task( parse(msg.record.value).extract[V] ),msg)
    }
  }

}
