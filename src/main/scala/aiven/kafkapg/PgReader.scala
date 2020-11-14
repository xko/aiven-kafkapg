package aiven.kafkapg

import java.io.File
import java.sql.Timestamp
import java.time.Instant

import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Using

object PgReader extends App {
  def getEntries(limit: Int): DBIO[Seq[(String, Double, Timestamp)]] =
    sql"SELECT * FROM os_metrics limit $limit".as[(String, Double, Timestamp)]
  Using( Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File("pg.client.properties"))) ){ db =>
    val a: DBIO[Unit] = getEntries(50).map(_.foreach(println))
    val f: Future[Unit] = db.run(a)
    Await.result(f, Duration.Inf)
  }

}
