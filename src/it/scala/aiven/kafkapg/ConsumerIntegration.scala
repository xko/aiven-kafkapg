package aiven.kafkapg

import aiven.kafkapg.KafkaConsumer._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.json4s._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{Random, Try}

case class TestOrder(name:String, amount: Long)
case class TestPerson(firstName: String, lastName: String)

class ConsumerIntegration extends AsyncFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers  {
  implicit val ser: Serialization = org.json4s.jackson.Serialization
  implicit val formats: Formats = DefaultFormats

  val test_orders = "test_orders"

  val admin: AdminClient = AdminClient.create(KafkaConsumer.defaultConfig.toJavaMap)

  override protected def beforeEach(): Unit = Try {
      admin.deleteTopics(List(test_orders).asJava).all().get(10, SECONDS)
      admin.createTopics(List(new NewTopic(test_orders, 1, 2.toShort)).asJava).all().get(10, SECONDS)
  }

  override protected def afterAll(): Unit = admin.close()

  "fragile" should "consume once" in {
    val order = TestOrder("water chip", Random.nextLong(100000))
    val publish = KafkaPublisher.publish(Observable.eval(order),test_orders)
    val consume = fragile(json[TestOrder](test_orders,"delivery")).mapEval(commit).firstL
    publish.flatMap(_ => consume).map { res =>
      res should === (order)
    }.timeout(30.seconds).runToFuture.flatMap{_=>
      recoverToSucceededIf[TimeoutException]( consume.timeout(10.seconds).runToFuture )
    }
  }

  "fragile" should "fail on wrong message" in {
    val joe = TestPerson("Joe", "Doe")
    val publish = KafkaPublisher.publish[TestPerson](Observable.eval(joe), test_orders).timeout(20.seconds)
    val consumeWrong = fragile(json[TestOrder](test_orders, "delivery")).mapEval(commit).firstL
    publish.runToFuture.flatMap { _ =>
      recoverToSucceededIf[MappingException](consumeWrong.runToFuture)
    }
  }

  "fragile" should "fail on wrong message, then succeed" in {
    val joe = TestPerson("Joe", "Doe")
    val publish = KafkaPublisher.publish[TestPerson](Observable.eval(joe), test_orders).timeout(20.seconds)
    val consumeWrong = fragile(json[TestOrder](test_orders, "delivery")).mapEval(commit).firstL
    val consumeRight = fragile(json[TestPerson](test_orders, "delivery")).mapEval(commit).firstL
    publish.runToFuture.flatMap { _ =>
        recoverToExceptionIf[MappingException](consumeWrong.runToFuture)
    }.flatMap( _ => consumeRight.runToFuture ).map { res =>
        res should ===(joe)
    }
  }

  "careless" should "should ignore wrong message" in {
    val joe = TestPerson("Joe", "Doe")
    val chip = TestOrder("water chip", Random.nextLong(100000))
    val publishWrong = KafkaPublisher.publish(Observable.eval(joe),test_orders)
    val publishRight = KafkaPublisher.publish(Observable.eval(chip),test_orders)
    val consume = careless(json[TestOrder](test_orders, "delivery")).mapEval(commit).firstL
    publishWrong.flatMap(_=> publishRight).flatMap(_ => consume).map {res =>
      res should === (chip)
    }.runToFuture
  }
}
