package aiven.kafkapg

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.json4s.{DefaultFormats, Formats, Serialization}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import java.time.temporal.ChronoUnit
import slick.jdbc.PostgresProfile.api._
import KafkaConsumer._
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import scala.jdk.CollectionConverters._
import Postgres._

import scala.util.{Random, Try}

class PostgresIntegration extends AsyncFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers {
  implicit val ser: Serialization = org.json4s.jackson.Serialization
  implicit val formats: Formats = DefaultFormats

  val admin: AdminClient = AdminClient.create(KafkaConsumer.defaultConfig.toJavaMap)

  override protected def beforeEach(): Unit = Try {
      admin.deleteTopics(List(topic).asJava).all().get(10, SECONDS)
      admin.createTopics(List(new NewTopic(topic, 1, 2.toShort)).asJava).all().get(10, SECONDS)
  }

  override protected def afterAll(): Unit =  admin.close()

  val topic = "test_pg"

  it should "store to db once" in {
    implicit val formats: Formats = Json.formats
    val metrics = OsMetrics.initial.copy(hostName = "apparat-" + Random.nextInt(100000))
    val expected = metrics.copy(timestamp = metrics.timestamp.truncatedTo(ChronoUnit.MILLIS)) //pg doesn't keep nanos
    val toKafka = KafkaPublisher.publish(Observable.eval(metrics),topic)
    val kafkaToPg = insistent { inDb { pg => // will retry on pg errors
      fragile { // deserialization will fail fast
        fromJson[OsMetrics](topic, "pg_writer")
      }.map { msg =>
        msg.wrap( pg.task(OsMetricsTable.query += msg.value) )
      }
    }}.mapEval(commit)
    val fromPg = inDb(_.stream(OsMetricsTable.queryBy(Some(metrics.hostName)).result)).firstL
    toKafka.flatMap(_ => kafkaToPg.firstL).flatMap(_ => fromPg).map { res =>
      res.head should === (expected)
    }.timeout(20.seconds).runToFuture
  }
}
