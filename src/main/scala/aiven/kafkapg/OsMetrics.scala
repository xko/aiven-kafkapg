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

    import AutoOption._
    copy( Instant.now(),
          OsMetrics.cpu.getSystemCpuLoadBetweenTicks(cpuTicks), OsMetrics.freeMemBytes,
          OsMetrics.topCPUProcess.getName, OsMetrics.topMemProcess.getName,
          ((newInBytes - netIOBytes._1) / dsec).toLong, ((newOutBytes - netIOBytes._2) / dsec ).toLong)
  }
  private val cpuTicks: Array[Long] = OsMetrics.cpu.getSystemCpuLoadTicks
  private val netIOBytes = OsMetrics.netIOBytes
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

  implicit val schematic: Schematic[OsMetrics] = new Schematic[OsMetrics] {
    val schema: Schema = SchemaBuilder.struct()
      .field("timestamp", Timestamp.SCHEMA)
      .field("hostName", Schema.STRING_SCHEMA)
      .field("cpuLoad", Schema.OPTIONAL_FLOAT64_SCHEMA).field("freeMemBytes", Schema.OPTIONAL_INT64_SCHEMA)
      .field("topCPUProcess", Schema.OPTIONAL_STRING_SCHEMA).field("topMemProcess", Schema.OPTIONAL_STRING_SCHEMA)
      .field("netInBytesPerS", Schema.OPTIONAL_INT64_SCHEMA).field("netOutBytesPerS", Schema.OPTIONAL_INT64_SCHEMA)
      .build()
  }

}

