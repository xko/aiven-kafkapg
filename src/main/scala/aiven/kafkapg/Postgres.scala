package aiven.kafkapg

import aiven.kafkapg.Postgres.defaultConfig
import com.typesafe.config.{Config, ConfigFactory}
import monix.eval.Task
import monix.reactive.Observable
import slick.jdbc.PostgresProfile.api._

import java.io.File


object Postgres  {
  val defaultConfig: Config = ConfigFactory.parseFileAnySyntax(new File(".pg/client.properties"))

  def inDb[R](config: Config)(action:Postgres  => Observable[R]): Observable[R] =
    Observable.eval(Database.forConfig("",config))
              .bracket(db => action(Postgres(db)))( db => Task(db.close()) )

  def inDb[R](action:Postgres  => Observable[R]): Observable[R] = inDb(defaultConfig)(action)

}

case class Postgres private(db: Database)  {

  def task[R](q:DBIO[R]):Task[R] = Task.deferFuture(db.run(q))

  def stream[R](qs:DBIO[R]*): Observable[R] = Observable.fromIterable(qs).mapEval(task)

}




