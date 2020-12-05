package aiven.kafkapg

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.json4s.Formats
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._
import java.time.temporal.ChronoUnit
import slick.jdbc.PostgresProfile.api._

import scala.util.Random

class PostgresIntegration extends AsyncFlatSpec with Matchers {

  it should "store to db once" in {
    implicit val formats: Formats = Json.formats
    val pg = Postgres()
    val topic = "test_pg"
    val metrics = OsMetrics.initial.copy(hostName = "apparat-" + Random.nextInt(100000))
    val expected = metrics.copy(timestamp = metrics.timestamp.truncatedTo(ChronoUnit.MILLIS)) //pg doesn't keep nanos
    val toKafka = KafkaPublisher.publish(Observable.eval(metrics),topic)
    val kafkaToPg = pg.streamRun(KafkaConsumer.consume[OsMetrics](topic, "pg_writer")){ v =>
      OsMetricsTable.query += v
    }
    val fromPg = pg.run( OsMetricsTable.queryBy(Some(metrics.hostName)).result ).map(_.head)
    toKafka.flatMap(_ => kafkaToPg.take(1).firstL).flatMap(_ => fromPg).map { res =>
      res should === (expected)
    }.timeout(10.seconds).runToFuture
  }
}
