import scoverage.ScoverageKeys

name := "gfc-util"

organization := "com.gilt"

scalaVersion := "2.12.6"

crossScalaVersions := Seq(scalaVersion.value, "2.11.12", "2.10.7")

scalacOptions += "-target:jvm-1.7"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

libraryDependencies ++= Seq(
  "com.gilt" %% "gfc-collection" % "0.0.5",
  "commons-codec" % "commons-codec" % "1.11",
  "com.gilt" %% "gfc-time" % "0.0.7" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)

ScoverageKeys.coverageFailOnMinimum := true

ScoverageKeys.coverageMinimum := 95.7

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache-style" -> url("https://raw.githubusercontent.com/gilt/gfc-util/master/LICENSE"))

homepage := Some(url("https://github.com/gilt/gfc-util"))

pomExtra := (
  <scm>
    <url>https://github.com/gilt/gfc-util.git</url>
    <connection>scm:git:git@github.com:gilt/gfc-util.git</connection>
  </scm>
  <developers>
    <developer>
      <id>gheine</id>
      <name>Gregor Heine</name>
      <url>https://github.com/gheine</url>
    </developer>
    <developer>
      <id>ebowman</id>
      <name>Eric Bowman</name>
      <url>https://github.com/ebowman</url>
    </developer>
    <developer>
      <id>andreyk0</id>
      <name>Andrey Kartashov</name>
      <url>https://github.com/andreyk0</url>
    </developer>
  </developers>
)

