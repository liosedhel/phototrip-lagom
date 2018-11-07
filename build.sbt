organization in ThisBuild := "pl.liosedhel"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire   = "com.softwaremill.macwire" %% "macros"    % "2.3.0" % "provided"
val scalaTest = "org.scalatest"            %% "scalatest" % "3.0.4" % Test

lazy val `mytrip` = (project in file("."))
  .aggregate(
    `mytrip-user-api`,
    `mytrip-worldmap-api`,
    `mytrip-worldmap-impl`,
    `mytrip-worldmapstream-api`,
    `mytrip-worldmapstream-impl`
  )

lazy val `mytrip-user-api` = (project in file("mytrip-user-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPubSub
    )
  )

lazy val `mytrip-worldmap-api` = (project in file("mytrip-worldmap-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPubSub,
      filters
    )
  )

lazy val `mytrip-worldmap-impl` = (project in file("mytrip-worldmap-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`mytrip-worldmap-api`)

lazy val `mytrip-worldmapstream-api` = (project in file("mytrip-worldmapstream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `mytrip-worldmapstream-impl` = (project in file("mytrip-worldmapstream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      filters
    )
  )
  .dependsOn(`mytrip-worldmapstream-api`, `mytrip-worldmap-api`)

lazy val `mytrip-comments-api` = (project in file("mytrip-comments-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPubSub,
      filters
    )
  ).dependsOn(`mytrip-worldmap-api`)

lazy val `mytrip-comments-impl` = (project in file("mytrip-comments-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`mytrip-comments-api`)
