package aiven.kafkapg

import java.time.Instant
import slick.jdbc.PostgresProfile.api._

class OsMetricsTable(tag: Tag) extends Table[OsMetrics](tag, "os_metrics") {
  def timestamp = column[Instant](OsMetrics.timestamp_)
  def cpuLoad = column[Option[Double]](OsMetrics.cpuLoad_)
  def freeMemBytes = column[Option[Long]](OsMetrics.freeMemBytes_)
  def topCPUProcess = column[Option[String]](OsMetrics.topCPUProcess_)
  def topMemProcess = column[Option[String]](OsMetrics.topMemProcess_)
  def netInBytesPerS = column[Option[Long]](OsMetrics.netInBytesPerS_)
  def netOutBytesPerS = column[Option[Long]](OsMetrics.netOutBytesPerS_)
  def hostName = column[String](OsMetrics.hostName_)

  def * = (timestamp, cpuLoad, freeMemBytes,
    topCPUProcess, topMemProcess,
    netInBytesPerS, netOutBytesPerS, hostName).<>((OsMetrics.apply _).tupled, OsMetrics.unapply)
}

object OsMetricsTable {
  def query(host: Option[String]): Query[OsMetricsTable, OsMetrics, Seq] = {
    host.foldLeft(TableQuery[OsMetricsTable].sortBy(_.timestamp.desc)){ (q, host) =>
      q.filter(_.hostName === host)
    }
  }
}