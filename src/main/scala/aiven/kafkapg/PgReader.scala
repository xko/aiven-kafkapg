package aiven.kafkapg

import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.PostgresProfile.api._

import java.io.File
import scala.util.Using


object PgReader  {
  val defaultConfig: Config = ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))

  def inDb[R](config:Config = defaultConfig)(action: Database => R):R =
    Using.resource( Database.forConfig("", config) )(action)

}




