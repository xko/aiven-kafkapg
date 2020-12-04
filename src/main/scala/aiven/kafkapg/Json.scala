package aiven.kafkapg

import org.json4s.{CustomSerializer, DefaultFormats, Formats, JInt, JLong}

import java.time.Instant

object Json {
  val InstantAsLong = new CustomSerializer[Instant]( _ => (
    { case ms: JLong => Instant.ofEpochMilli(ms.values)
      case ms: JInt => Instant.ofEpochMilli(ms.values.toLong) },
    { case ms: Instant => JLong(ms.toEpochMilli) }
  ))
  val formats: Formats = DefaultFormats + InstantAsLong
}
