val commonSettings = Seq(
  crossScalaVersions := Vector("2.11.12", "2.12.8"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-encoding", "utf-8", "-feature"),
  resolvers ~= {
    _.filterNot(_.name.toLowerCase.contains("codacy"))
  },
  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies += "net.openhft" % "chronicle-queue" % "5.17.8"
)

lazy val akkaVersion = settingKey[String]("Akka version to compile with")

// ThisBuild / akkaVersion := "2.4.20"
ThisBuild / akkaVersion := "2.5.20"

lazy val chronicleQueueStream = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publicMvnPublish)
  .settings(
    name := "chronicle-queue-stream",
    javacOptions in Compile ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion.value % "provided",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion.value % "provided",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion.value % "test",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
    ),
    description := "Akka Stream backed by Chronicle queue",
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/codacy/chronicle-queue-stream"),
        "scm:git@github.com:codacy/chronicle-queue-stream.git"
      )
    ),
    // this setting is not picked up properly from the plugin
    pgpPassphrase := Option(System.getenv("SONATYPE_GPG_PASSPHRASE"))
      .map(_.toCharArray),
    resolvers ~= { _.filterNot(_.name.toLowerCase.contains("codacy")) }
  )

cancelable in Global := true
fork in run := true
fork in Test := true
