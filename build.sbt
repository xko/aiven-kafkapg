name := "aiven-kafkapg"
version := "0.1"

scalaVersion := "2.13.3"
scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "com.github.oshi" % "oshi-core" % "5.3.4"

libraryDependencies += "io.monix" %% "monix" % "3.3.0"
libraryDependencies += "io.monix" %% "monix-kafka-1x" % "1.0.0-RC6"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30" % Runtime

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "org.postgresql" % "postgresql" % "42.2.18" % Runtime
)

libraryDependencies += "org.apache.kafka" % "connect-api" % "2.6.0"
libraryDependencies += "org.apache.kafka" % "connect-json" % "2.6.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.6.0" % IntegrationTest

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.7.0-M7"
libraryDependencies += "io.github.azhur" %% "kafka-serde-json4s" % "0.5.0"

configs(IntegrationTest)
Defaults.itSettings
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test,it"
IntegrationTest / parallelExecution := false
//IntegrationTest / fork := true