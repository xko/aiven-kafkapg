package aiven.kafkapg

import java.io.File

import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object PgReader extends App {
  val db = Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File("pg.client.properties")))
  def getEntries(limit: Int): DBIO[Seq[(String, String)]] =
    sql"SELECT * FROM os_metrics limit $limit".as[(String,String)]

  try {
    val a: DBIO[Unit] = getEntries(50).map(_.foreach(println))
    val f: Future[Unit] = db.run(a)
    Await.result(f, Duration.Inf)
  } finally db.close

}
