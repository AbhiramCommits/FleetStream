name := "FleetStream"
version := "0.1.0"
scalaVersion := "2.13.14"

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "com.fleetstream"

val akkaVersion = "2.8.5"
val akkaHttpVersion = "10.5.3"
val influxClientVersion = "6.10.0"
val postgresVersion = "42.7.2"
val hikariVersion = "5.1.0"
val flywayVersion = "10.4.1"
val scalaTestVersion = "3.2.18"
val logbackVersion = "1.4.14"
val jacksonVersion = "2.13.4"

lazy val root = (project in file("."))
  .aggregate(core, cluster, loadgen)
  .settings(
    name := "FleetStream-root",
    publish / skip := true
  )

lazy val core = (project in file("core"))
  .settings(
    name := "fleetstream-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "com.influxdb" % "influxdb-client-java" % influxClientVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )

lazy val cluster = (project in file("cluster"))
  .dependsOn(core)
  .settings(
    name := "fleetstream-cluster",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "org.postgresql" % "postgresql" % postgresVersion,
      "com.zaxxer" % "HikariCP" % hikariVersion,
      "org.flywaydb" % "flyway-core" % flywayVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )

lazy val loadgen = (project in file("loadgen"))
  .dependsOn(core, cluster)
  .settings(
    name := "fleetstream-loadgen",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "org.hdrhistogram" % "HdrHistogram" % "2.2.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )
