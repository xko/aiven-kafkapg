package aiven.kafkapg

import PgReader._
import monix.reactive.Observable

import java.time.Instant
import java.time.temporal.ChronoUnit
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global

trait MainCanWait {
  def main(args: Array[String]): Unit = {
    Await.ready(go(args), Duration.Inf).value.get.get
  }

  def go(args: Array[String]) : Future[Unit]
}

object FromPgLast10Records extends MainCanWait {
  def go(args:Array[String]): Future[Unit] = inDb()(_.run(OsMetricsTable.query(args.headOption).take(10).result))
                                             .map(r => println(r.mkString("\n")))
}

object FromPgAvgCPULastHour extends MainCanWait {
  override def go(args: Array[String]): Future[Unit] = inDb()(_.run(
    OsMetricsTable.query(args.headOption)
                  .filter(_.timestamp > Instant.now().minus(1, ChronoUnit.HOURS))
                  .map(_.cpuLoad).avg.result
  )).map(println)
}

object ToKafkaConnectEvery3s extends MainCanWait {
  override def go(args: Array[String]): Future[Unit] =
    KafkaPublisher.publish4KConnect(
      Observable.interval(3.second).scan(OsMetrics.initial)( (metrics, _) => metrics.next )
    ).runToFuture
}