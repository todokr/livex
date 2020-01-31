lazy val root = (project in file("."))
  .settings(
    name := "livex",
    version := "1.0",
    scalaVersion := "2.13.1",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Xlint",
      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions"
    ),
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe-launcher" % "20.1.2",
      "org.scala-sbt"      %% "io"                % "1.3.1",
      "com.typesafe"       % "config"             % "1.4.0"
    )
  )
  .enablePlugins(SbtTwirl)

TwirlKeys.templateImports := Seq.empty
