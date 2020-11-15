package aiven.kafkapg

object AutoOption {
  import scala.language.implicitConversions
  implicit def autoOption[T](v: T): Option[T] = Option.apply(v)
}
