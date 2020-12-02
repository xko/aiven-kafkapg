package aiven.kafkapg

import aiven.kafkapg.PgReader._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration._

trait MainCanWait { //TODO: Why didn't monix.eval.TaskApp work?
  def main(args: Array[String]): Unit = {
    Await.result(go(args).runToFuture, Duration.Inf)
  }

  def go(args: Array[String]): Task[Unit]
}

object FromPgLast10Records extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] =
    inDb()(runQ( OsMetricsTable.query(args.headOption).take(10).result ))
      .map(_.mkString("\n")).map(println)
}

object FromPgAvgCPULastHour extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] =
    inDb()(runQ(
      OsMetricsTable.query(args.headOption)
                    .filter(_.timestamp > Instant.now().minus(1, ChronoUnit.HOURS))
                    .map(_.cpuLoad).avg.result
    )) .map(println)
}

object ToKafkaConnectEvery3s extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] =
    KafkaPublisher.publish4KConnect(
      Observable.interval(3.second).scan(OsMetrics.initial)( (metrics, _) => metrics.next )
    )
}