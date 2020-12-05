package aiven.kafkapg

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.apache.kafka.common.errors.SerializationException
import org.json4s._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.Random

case class TestOrder(name:String, amount: Long)
case class TestPerson(firstName: String, lastName: String)

class ConsumerIntegration extends AsyncFlatSpec  with Matchers  {

  it should "consume once" in {
    implicit val formats: Formats = DefaultFormats
    val topic = "test_orders"
    val order = TestOrder("water chip", Random.nextLong(100000))
    val publish = KafkaPublisher.publish(Observable.eval(order),topic)
    val consume = KafkaConsumer.consume[TestOrder](topic, "delivery").firstL
    publish.flatMap(_ => consume).map { res =>
      res should === (order)
    }.timeout(10.seconds).runToFuture.flatMap{_=>
      recoverToSucceededIf[TimeoutException]( consume.timeout(10.seconds).runToFuture )
    }
  }

  it should "fail consume with wrong des7er, then succeed" in {
    implicit val formats: Formats = DefaultFormats
    val topic = "test_orders"
    val joe = TestPerson("Joe", "Doe")
    val publish = KafkaPublisher.publish(Observable.eval(joe),topic).timeout(10.seconds)
    val consumeWrong = KafkaConsumer.consume[TestOrder](topic, "delivery").firstL.timeout(10.seconds)
    val consumeRight = KafkaConsumer.consume[TestPerson](topic, "delivery").firstL.timeout(10.seconds)
    publish.runToFuture.flatMap { _ =>
      recoverToSucceededIf[SerializationException]( consumeWrong.runToFuture )
    } flatMap { _ =>
      consumeRight.runToFuture
    } map { res =>
      res should === (joe)
    }
  }

}
