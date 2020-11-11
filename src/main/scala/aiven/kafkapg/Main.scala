package aiven.kafkapg


import java.net.InetAddress

import oshi.SystemInfo
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.kafka._
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Await
import scala.concurrent.duration._


object Main {
  def main(args: Array[String]): Unit = {
    val si = new SystemInfo
    val cpu = si.getHardware.getProcessor
    val hostName = InetAddress.getLocalHost.getHostName

    val cpuLoads = Observable.interval(1.second).scan(0d,cpu.getSystemCpuLoadTicks) { case ( (_,ticksBefore), _) =>
      (cpu.getSystemCpuLoadBetweenTicks(ticksBefore), cpu.getSystemCpuLoadTicks)
    }.map(_._1)
    val records = cpuLoads.map(
      l => new ProducerRecord[String,String]("test",null,s"""{"host":"$hostName", "cpuLoad":"$l"}""")
    )

    val producerCfg = KafkaProducerConfig.default.copy( List("localhost:9092") )
    implicit val scheduler: Scheduler = monix.execution.Scheduler.global

    val producer = KafkaProducerSink[String,String](producerCfg, scheduler)
    val doit = records.bufferIntrospective(1024).consumeWith(producer).runToFuture
    Await.result(doit, Duration.Inf)
  }
}
