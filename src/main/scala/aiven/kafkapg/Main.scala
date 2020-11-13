package aiven.kafkapg


import scala.concurrent.Await
import scala.concurrent.duration._


object Main {
  def main(args: Array[String]): Unit = {
    Await.result(Publisher.publishCpuLoad, Duration.Inf)
  }

}
