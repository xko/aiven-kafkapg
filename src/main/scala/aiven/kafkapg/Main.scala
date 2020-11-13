package aiven.kafkapg


import java.io.File
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
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
    val schema = """ {
                   |    "type": "struct",
                   |    "fields": [
                   |        { "field": "host", "type": "string", "optional": false },
                   |        { "field": "cpuLoad", "type": "string", "optional": false }
                   |    ]
                   |  }""".stripMargin

    val cpuLoads = Observable.interval(3.second).scan(0d,cpu.getSystemCpuLoadTicks) { case ( (_,ticksBefore), _) =>
      (cpu.getSystemCpuLoadBetweenTicks(ticksBefore), cpu.getSystemCpuLoadTicks)
    }.map(_._1)
    val records = cpuLoads.map( l => {
        val msg =
          s"""{"schema": $schema,
             | "payload": {"host":"$hostName", "cpuLoad":"$l"}
             | }""".stripMargin
        println(msg)
        new ProducerRecord[String,String]("os-metrics-log",null,msg)
      }
    )

    val producerCfg = KafkaProducerConfig(ConfigFactory.parseFileAnySyntax(new File("client.properties")))
    implicit val scheduler: Scheduler = monix.execution.Scheduler.global

    val producer = KafkaProducerSink[String,String](producerCfg, scheduler)
    val doit = records.bufferIntrospective(1024).consumeWith(producer).runToFuture
    Await.result(doit, Duration.Inf)
  }
}
