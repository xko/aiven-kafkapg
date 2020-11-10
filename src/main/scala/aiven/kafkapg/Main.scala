package aiven.kafkapg


import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import oshi.SystemInfo

import scala.concurrent.Await


object Main {
  def main(args: Array[String]): Unit = {
    val cpu = (new SystemInfo).getHardware.getProcessor
    val f = Observable.interval(1.second).scan(0d,cpu.getSystemCpuLoadTicks) { (prev, i) =>
      (cpu.getSystemCpuLoadBetweenTicks(prev._2),cpu.getSystemCpuLoadTicks )
    }
    Await.result(f.dump("CPU Load").foreach(_=>()), Duration.Inf)
  }
}
