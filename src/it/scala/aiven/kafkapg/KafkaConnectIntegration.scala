package aiven.kafkapg

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.PostgresProfile.api._
import Postgres._

import java.time.temporal.ChronoUnit
import scala.util.Random

class KafkaConnectIntegration extends AsyncFlatSpec with Matchers  {
  it should "deliver to postgres" in {
    val metrics = OsMetrics.initial.copy(hostName = "machine-" + Random.nextInt(100000))
    val publish = KafkaPublisher.publish4KConnect(Observable.eval(metrics), OsMetrics.topic4KafkaConnect)
    val readPg = inDb(_.stream(OsMetricsTable.queryBy(None).filter(_.hostName === metrics.hostName).result)).firstL
    val expected = metrics.copy(timestamp = metrics.timestamp.truncatedTo(ChronoUnit.MILLIS)) //pg doesn't keep nanos
    val verify = readPg.map(_.head should === (expected) ).onErrorRestart(10)
    publish.flatMap(_ => verify).runToFuture
  }

}
