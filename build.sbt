name := "aiven-kafkapg"
version := "0.1"

scalaVersion := "2.13.3"
scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "com.github.oshi" % "oshi-core" % "5.3.4"

libraryDependencies += "io.monix" %% "monix" % "3.3.0"
libraryDependencies += "io.monix" %% "monix-kafka-1x" % "1.0.0-RC6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % Test

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30" % Runtime

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "org.postgresql" % "postgresql" % "42.2.18" % Runtime
)
