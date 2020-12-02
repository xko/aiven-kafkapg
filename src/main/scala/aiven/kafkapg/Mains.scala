package aiven.kafkapg

import PgReader._
import monix.reactive.Observable

import java.time.Instant
import java.time.temporal.ChronoUnit
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

import monix.execution.Scheduler.Implicits.global

object FromPgLast10Records extends App {
  inDb(OsMetricsTable.query(args.headOption).take(10).result){ r =>
    println(r.mkString("\n"))
  }
}

object FromPgAvgCPULastHour extends App {
  inDb( OsMetricsTable.query(args.headOption)
          .filter(_.timestamp > Instant.now().minus(1, ChronoUnit.HOURS))
          .map(_.cpuLoad).avg.result ) { r =>
    println(r)
  }
}

object ToKafkaConnectEvery3s extends App {
  val go = KafkaPublisher.publish4KConnect( Observable.interval(3.second).scan(OsMetrics.initial )
                                          ( (metrics, _) => metrics.next) ).runToFuture
  Await.ready(go, Duration.Inf).value.get.get
}