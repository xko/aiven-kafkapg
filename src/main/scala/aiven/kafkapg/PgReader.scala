package aiven.kafkapg

import java.io.File
import java.time.Instant

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Using}

object PgReader extends App {
  Using( Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))) ){ db =>
    val a = TableQuery[OsMetricsTable].sortBy(_.timestamp.desc).take(10).result
    val res = Await.ready(db.run(a), Duration.Inf).value.get
    res match {
      case Success(recs) => println(recs.mkString("\n"))
      case Failure(err) => err.printStackTrace()
    }
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
            netInBytesPerS, netOutBytesPerS, hostName ) <> ((OsMetrics.apply _).tupled, OsMetrics.unapply )
}

