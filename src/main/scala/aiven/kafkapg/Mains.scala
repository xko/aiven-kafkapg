package aiven.kafkapg

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.json4s.{Formats, Serialization}
import slick.jdbc.PostgresProfile.api._
import Postgres._
import aiven.kafkapg.KafkaConsumer._

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

trait MainCanWait { //TODO: Why didn't monix.eval.TaskApp work?
  def main(args: Array[String]): Unit = {
    val future = go(args).runToFuture
    try {
      Await.result(future, Duration.Inf)
    } finally {
      if(!future.isCompleted) future.cancel()
    }
  }

  def go(args: Array[String]): Task[Unit]
}

object FromPgLast10Records extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] = {
    inDb(_.stream(OsMetricsTable.queryBy(args.headOption).take(10).result)).firstL
      .map(_.mkString("\n") ).map(println)
  }
}

object FromPgAvgCPULastHour extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] =
    inDb(_.stream(OsMetricsTable.queryBy(args.headOption)
                 .filter(_.timestamp > Instant.now().minus(1, ChronoUnit.HOURS))
                 .map(_.cpuLoad).avg.result)).firstL.map(println)
}

object ToKafkaConnectEvery3s extends MainCanWait {
  override def go(args: Array[String]): Task[Unit] =
    KafkaPublisher.publish4KConnect(
      Observable.interval(3.second).scan(OsMetrics.initial)( (metrics, _) => metrics.next ),
      OsMetrics.topic4KafkaConnect
    )
}

object ToKafkaEvery3s extends MainCanWait {
  implicit val fmt: Formats = Json.formats
  override def go(args: Array[String]): Task[Unit] =
    KafkaPublisher.publish(
      Observable.interval(3.second).scan(OsMetrics.initial)( (metrics, _) => metrics.next ),
      OsMetrics.topicBareJson
    )
}

case class Noise(random: Long, randomer: Long)
object NoiseToKafkaEvery5s extends MainCanWait {
  implicit val fmt: Formats = Json.formats
  override def go(args: Array[String]): Task[Unit] =
    KafkaPublisher.publish(
      Observable.interval(5.second).map(_ => Noise(Random.nextLong(), Random.nextLong() + Random.nextLong())),
      OsMetrics.topicBareJson
    )
}

object FromKafkaToConsole extends MainCanWait {
  implicit val fmt: Formats = Json.formats
  implicit val ser: Serialization = org.json4s.jackson.Serialization
  override def go(args: Array[String]): Task[Unit] = {
    val groupId = "console"+Random.nextLong(100000) // can run many of these
    carelessHonest(fromJson[OsMetrics](OsMetrics.topicBareJson, groupId)).mapEval(commit).dump("Received:").completedL
  }
}

object FromKafkaToPg extends MainCanWait {
  implicit val fmt: Formats = Json.formats
  implicit val ser: Serialization = org.json4s.jackson.Serialization
  override def go(args: Array[String]): Task[Unit] = {
    inDb { pg => insistent {  // will retry on pg errors, but not on failed connection
      fragile { // deserialization will fail fast
        fromJson[OsMetrics](OsMetrics.topicBareJson, OsMetrics.pgSinkGroupId)
      }.map { m =>
        m.wrap(pg.task(OsMetricsTable.query += m.value))
      }
    }}.mapEval(commit).dump("Received:").completedL
  }
}

object FromKafkaToPgCareless extends MainCanWait {
  implicit val fmt: Formats = Json.formats
  implicit val ser: Serialization = org.json4s.jackson.Serialization
  override def go(args: Array[String]): Task[Unit] = {
    inDb { pg => insistent {  // will retry on pg errors, but not on failed connection
      carelessHonest { // will ignore and log de12n errors
        fromJson[OsMetrics](OsMetrics.topicBareJson, OsMetrics.pgSinkGroupId)
      }.map { m =>
        m.wrap(pg.task(OsMetricsTable.query += m.value))
      }
    }}.mapEval(commit).dump("Received:").completedL
  }
}