package aiven.kafkapg

import java.io.File
import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

import com.typesafe.config.ConfigFactory
import slick.dbio.NoStream
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Using}


object PgReader  {
  def inDb[R](a:DBIOAction[R, NoStream, Nothing])(onRes: R=>Unit) =
    Using( Database.forConfig("",ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))) ){ db =>
      val res = Await.ready(db.run(a), Duration.Inf).value.get
      res match {
        case Success(r) => onRes(r)
        case Failure(err) => err.printStackTrace()
      }
    }

}




