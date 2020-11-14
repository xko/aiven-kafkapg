package aiven.kafkapg

import java.io.File
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import monix.execution.{CancelableFuture, Scheduler}
import monix.kafka.{KafkaProducerConfig, KafkaProducerSink}
import monix.reactive.Observable
import org.apache.kafka.clients.producer.ProducerRecord
import oshi.SystemInfo

import scala.concurrent.duration._


object Publisher {
  def publishCpuLoad: CancelableFuture[Unit] = {
    val si = new SystemInfo
    val cpu = si.getHardware.getProcessor
    val hostName = InetAddress.getLocalHost.getHostName
    val schema =
      """ {
        |    "type": "struct",
        |    "fields": [
        |        { "field": "host", "type": "string", "optional": false },
        |        { "field": "cpuLoad", "type": "string", "optional": false }
        |    ]
        |  }""".stripMargin

    val cpuLoads = Observable.interval(3.second).scan(0d, cpu.getSystemCpuLoadTicks) { case ((_, ticksBefore), _) =>
      (cpu.getSystemCpuLoadBetweenTicks(ticksBefore), cpu.getSystemCpuLoadTicks)
    }.map(_._1)
    val records = cpuLoads.map(l => {
      val msg =
        s"""{"schema": $schema,
           | "payload": {"host":"$hostName", "cpuLoad":"$l"}
           | }""".stripMargin
      println(msg)
      new ProducerRecord[String, String]("os_metrics", null, msg)
    }
    )

    val producerCfg = KafkaProducerConfig(ConfigFactory.parseFileAnySyntax(new File("client.properties")))
    implicit val scheduler: Scheduler = monix.execution.Scheduler.global

    val producer = KafkaProducerSink[String, String](producerCfg, scheduler)
    records.bufferIntrospective(1024).consumeWith(producer).runToFuture
  }
}
