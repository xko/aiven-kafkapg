package aiven.kafkapg

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.apache.kafka.common.errors.SerializationException
import org.json4s._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration._
import scala.util.Random

case class TestOrder(name:String, amount: Long)
case class TestPerson(firstName: String, lastName: String)

class ConsumerIntegration extends AnyFlatSpec with Matchers with Eventually with IntegrationPatience {

  it should "consume once" in {
    implicit val formats: Formats = DefaultFormats
    val topic = "test_orders"
    val order = TestOrder("water chip", Random.nextLong(100000))
    val pub = KafkaPublisher.publish(Observable.eval(order),topic)
    val consume = KafkaConsumer.consume[TestOrder](topic, "delivery").firstL
    Await.result(pub.runToFuture, 15.seconds)
    Await.result(consume.runToFuture, 15.second) should be(order)
    val timeoutFuture = consume.runToFuture
    a [TimeoutException] should be thrownBy Await.result(timeoutFuture, 15.seconds)
    timeoutFuture.cancel()
  }

  it should "fail consume with wrong des7er, then succeed" in {
    implicit val formats: Formats = DefaultFormats
    val topic = "test_orders"
    val joe = TestPerson("Joe", "Doe")
    val pub = KafkaPublisher.publish(Observable.eval(joe),topic)
    Await.result(pub.runToFuture, 10.seconds)
    val consumeWrong = KafkaConsumer.consume[TestOrder](topic, "delivery").firstL
    val consumeRight = KafkaConsumer.consume[TestPerson](topic, "delivery").firstL
    a [SerializationException] should be thrownBy Await.result(consumeWrong.runToFuture, 10.second)
    Await.result(consumeRight.runToFuture,10.seconds) should be (joe)
  }

}
