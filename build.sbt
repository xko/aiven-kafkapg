name := "aiven-kafkapg"
version := "0.1"

scalaVersion := "2.13.3"
scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "com.github.oshi" % "oshi-core" % "5.3.4"

libraryDependencies += "io.monix" %% "monix" % "3.3.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % Test

