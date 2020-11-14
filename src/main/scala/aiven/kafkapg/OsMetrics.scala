package aiven.kafkapg

import java.net.InetAddress
import java.time.Instant

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Timestamp}
import oshi.SystemInfo
import oshi.hardware.CentralProcessor



case class OsMetrics (timestamp: Instant, hostName: String, cpuLoad: Double ){
  def next: OsMetrics = OsMetrics( Instant.now(), OsMetrics.hostName,
                        OsMetrics.cpu.getSystemCpuLoadBetweenTicks(cpuTicks) )

  private val cpuTicks: Array[Long] = OsMetrics.cpu.getSystemCpuLoadTicks
}

object OsMetrics {
  lazy val si: SystemInfo = new SystemInfo
  lazy val cpu: CentralProcessor = si.getHardware.getProcessor
  lazy val hostName: String = InetAddress.getLocalHost.getHostName
  def initial: OsMetrics = OsMetrics(Instant.now(), hostName, 0d )

  implicit val schematic: Schematic[OsMetrics] = new Schematic[OsMetrics] {
    val schema: Schema = SchemaBuilder.struct().field("hostName",Schema.STRING_SCHEMA).field("cpuLoad",Schema.FLOAT64_SCHEMA)
                                               .field("timestamp",Timestamp.SCHEMA).build()
  }

}

