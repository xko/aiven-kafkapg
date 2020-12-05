package aiven.kafkapg

import aiven.kafkapg.Postgres.defaultConfig
import com.typesafe.config.{Config, ConfigFactory}
import monix.eval.Task
import monix.reactive.Observable
import slick.jdbc.PostgresProfile.api._

import java.io.File


object Postgres  {
  val defaultConfig: Config = ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))
}

case class Postgres(config: Config = defaultConfig)  {
  def db = Database.forConfig("",config)

  def task[R](action: Database => Task[R]): Task[R] = Task(db).bracket(action)(db => Task(db.close()))

  def run[R](q:DBIO[R]):Task[R] = task(db => Task.deferFuture(db.run(q)))

  def stream[R](action: Database => Observable[R]): Observable[R] =
    Observable.eval(db).bracket(action)( db => Task(db.close()) )

  def streamRun[S,R](source: Observable[S])(action: S => DBIO[R]): Observable[R] = stream { db =>
    source.mapEval( src => Task.deferFuture(db.run(action(src))) )
  }

}




