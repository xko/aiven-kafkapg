package aiven.kafkapg

import com.typesafe.config.{Config, ConfigFactory}
import monix.eval.Task
import slick.jdbc.PostgresProfile.api._

import java.io.File


object Postgres  {
  val defaultConfig: Config = ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))

  def inDb[R](config:Config = defaultConfig)(action: Database => Task[R]): Task[R] =
    Task.eval( Database.forConfig("",config) ).bracket(action)( db => Task(db.close()) )

  def runQ[R](thunk:DBIO[R])(db: Database) :Task[R] = Task.fromFuture(db.run(thunk))
}




