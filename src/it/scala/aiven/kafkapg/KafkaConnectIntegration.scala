package aiven.kafkapg

import aiven.kafkapg.Postgres._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.PostgresProfile.api._

import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class KafkaConnectIntegration extends AnyFlatSpec with Matchers{
  it should "reach postgres" in {
    val metrics = OsMetrics.initial.copy(hostName = "machine-"+Random.nextInt(100000))
    val publish = KafkaPublisher.publish4KConnect(Observable.eval(metrics),OsMetrics.kafkaTopic).runToFuture
    Await.result(publish, 10.seconds)
    Thread.sleep(3.seconds.toMillis)
    val readPg = inDb()(runQ(
      OsMetricsTable.query(None)
        .filter(_.hostName === metrics.hostName).result
      )).runToFuture
    val res = Await.result(readPg, 10.seconds)

    val expected = metrics.copy(timestamp = metrics.timestamp.truncatedTo(ChronoUnit.MILLIS)) //pg doesn't keep nanos
    res should contain(expected)
  }

}
