package aiven.kafkapg

import java.net.InetAddress
import java.time.{Duration, Instant}

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Timestamp}
import oshi.SystemInfo
import oshi.hardware.{CentralProcessor, NetworkIF}
import oshi.software.os.OperatingSystem.ProcessSort
import oshi.software.os.{OSProcess, OperatingSystem}

import scala.jdk.CollectionConverters._

case class OsMetrics ( timestamp: Instant,
                       cpuLoad: Option[Double], freeMemBytes: Option[Long],
                       topCPUProcess: Option[String], topMemProcess: Option[String],
                       netInBytesPerS: Option[Long], netOutBytesPerS: Option[Long],
                       hostName: String = OsMetrics.hostName ){

  def next: OsMetrics = {
    val dsec = Duration.between(timestamp, Instant.now).toNanos * 1e-9
    val (newInBytes,newOutBytes) = OsMetrics.netIOBytes
    copy( Instant.now(),
          Some(OsMetrics.cpu.getSystemCpuLoadBetweenTicks(cpuTicks)),
          Some(OsMetrics.freeMemBytes),
          Some(OsMetrics.topCPUProcess.getName), Some(OsMetrics.topMemProcess.getName),
          Some(((newInBytes - netInBytes) / dsec).toLong),
          Some(((newOutBytes - netOutBytes) / dsec ).toLong))
  }
  private val cpuTicks: Array[Long] = OsMetrics.cpu.getSystemCpuLoadTicks
  private val (netInBytes, netOutBytes) = OsMetrics.netIOBytes
}

object OsMetrics {
  lazy val hostName: String = InetAddress.getLocalHost.getHostName
  lazy val si: SystemInfo = new SystemInfo
  lazy val os: OperatingSystem = si.getOperatingSystem
  lazy val cpu: CentralProcessor = si.getHardware.getProcessor
  lazy val extNetworks: Seq[NetworkIF] = si.getHardware.getNetworkIFs(false).asScala.toSeq

  def netIOBytes: (Long, Long) = extNetworks.map { nif =>
    nif.updateAttributes()
    (nif.getBytesRecv, nif.getBytesSent)
  }.reduce { (acc, v) => (acc._1 + v._1, acc._2 + v._2) }

  def topCPUProcess: OSProcess = os.getProcesses(10, ProcessSort.CPU).asScala
                                   .toSeq.filter(_.getName.toLowerCase!="idle").head
  def topMemProcess: OSProcess = os.getProcesses(1, ProcessSort.MEMORY).asScala.head

  def freeMemBytes: Long = si.getHardware.getMemory.getAvailable

  def initial: OsMetrics = OsMetrics(Instant.now(), None, Some(freeMemBytes),
                                     Some(topCPUProcess.getName), Some(topMemProcess.getName), None, None)

  val timestamp_ = "timestamp"
  val hostName_ = "hostName"
  val cpuLoad_ = "cpuLoad"
  val freeMemBytes_ = "freeMemBytes"
  val topCPUProcess_ = "topCPUProcess"
  val topMemProcess_ = "topMemProcess"
  val netInBytesPerS_ = "netInBytesPerS"
  val netOutBytesPerS_ = "netOutBytesPerS"

  implicit object HaveSchema extends KafkaConnectJson.HasSchema[OsMetrics] {
    val schema: Schema = SchemaBuilder.struct()
      .field(timestamp_, Timestamp.SCHEMA)
      .field(hostName_, Schema.STRING_SCHEMA)
      .field(cpuLoad_, Schema.OPTIONAL_FLOAT64_SCHEMA).field(freeMemBytes_, Schema.OPTIONAL_INT64_SCHEMA)
      .field(topCPUProcess_, Schema.OPTIONAL_STRING_SCHEMA).field(topMemProcess_, Schema.OPTIONAL_STRING_SCHEMA)
      .field(netInBytesPerS_, Schema.OPTIONAL_INT64_SCHEMA).field(netOutBytesPerS_, Schema.OPTIONAL_INT64_SCHEMA)
      .build()
  }

  val topic4KafkaConnect: String = "os_metrics"
  val topicBareJson: String = "os_metrics_bare"
  val pgTable: String = "os_metrics"
  val pgSinkGroupId = "os_metrics_to_pg"
}

