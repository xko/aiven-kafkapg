package aiven.kafkapg

import java.io.File
import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

import com.typesafe.config.ConfigFactory
import slick.dbio.NoStream
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Using}


object PgReader  {
  def inDb[R](a:DBIOAction[R, NoStream, Nothing])(onRes: R=>Unit) =
    Using( Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))) ){ db =>
      val res = Await.ready(db.run(a), Duration.Inf).value.get
      res match {
        case Success(r) => onRes(r)
        case Failure(err) => err.printStackTrace()
      }
    }

  def queryByHostOrAll(host: Option[String]): Query[OsMetricsTable, OsMetrics, Seq] =
    host.fold(TableQuery[OsMetricsTable].sortBy(_.timestamp.desc)) { host =>
      TableQuery[OsMetricsTable].filter(_.hostName === host)
    }

}

import PgReader._

object Last10From extends App {
  inDb(queryByHostOrAll(args.headOption).take(10).result){ r =>
    println(r.mkString("\n"))
  }
}

object AvgCPULastHour extends App {
  inDb(queryByHostOrAll(args.headOption).filter(_.timestamp > Instant.now().minus(1, ChronoUnit.HOURS))
         .map(_.cpuLoad).avg.result){ r =>
    println(r)
  }
}

class OsMetricsTable(tag: Tag) extends Table[OsMetrics](tag, "os_metrics") {
  def timestamp = column[Instant]("timestamp")
  def cpuLoad = column[Option[Double]]("cpuLoad")
  def freeMemBytes = column[Option[Long]]("freeMemBytes")
  def topCPUProcess = column[Option[String]]("topCPUProcess")
  def topMemProcess = column[Option[String]]("topMemProcess")
  def netInBytesPerS = column[Option[Long]]("netInBytesPerS")
  def netOutBytesPerS = column[Option[Long]]("netOutBytesPerS")
  def hostName = column[String]("hostName")
  def * = ( timestamp, cpuLoad, freeMemBytes,
            topCPUProcess, topMemProcess,
            netInBytesPerS, netOutBytesPerS, hostName ).<>( (OsMetrics.apply _).tupled, OsMetrics.unapply )
}

