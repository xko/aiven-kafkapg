package aiven.kafkapg

import java.io.File
import java.sql.Timestamp
import java.time.Instant

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Using

object PgReader extends App {
  Using( Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File("pg.client.properties"))) ){ db =>
    val a = TableQuery[OsMetricsTable].take(50).result.map(_.foreach(println))
    Await.result(db.run(a), Duration.Inf)
  }
}

class OsMetricsTable(tag: Tag) extends Table[OsMetrics](tag, "os_metrics") {
  def hostName = column[String]("hostName")
  def timestamp = column[Instant]("timestamp")
  def cpuLoad = column[Double]("cpuLoad")
  def * = (timestamp, hostName, cpuLoad) <> ((OsMetrics.apply _).tupled, OsMetrics.unapply)
}

