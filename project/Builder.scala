import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import play.sbt.PlayImport._
import play.sbt.PlayScala
import sbtassembly.AssemblyPlugin.autoImport.{assemblyJarName, assemblyOutputPath}

object Builder {
  lazy val commonSettings = Seq(

    organization := "org.aelve",
    version := "0.1",
    scalaVersion := "2.12.4",

    scalacOptions := Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture",
      "-Xexperimental"
    ),
    scalacOptions in (Compile, console) -= "-Ywarn-unused-import",
    scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits"),
    scalacOptions in Test ++= Seq("-Yrangepos"),
  )

  lazy val commonDeps = Seq(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.7.0" ,
      "com.lihaoyi" %% "ammonite-ops" % "1.0.3",
      "org.rauschig" % "jarchivelib" % "0.7.1",
      "io.suzaku" %% "boopickle" % "1.3.0",
      "commons-io" % "commons-io" % "2.6",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    )
  )

  lazy val core = Project(id = "core", base = file("core"))
    .settings(commonSettings ++ commonDeps)
    .settings(name := "codesearch-core")


  lazy val webServer = Project(id = "web-server", base = file("web-server"))
    .settings(commonSettings ++ commonDeps)
    .settings(
      name := "codesearch-web-server",
      libraryDependencies ++= Seq(
        guice,
        "org.webjars"     % "bootstrap"        % "4.0.0",
        "javax.inject"    % "javax.inject"     % "1"
      )
    )
    .dependsOn(core)
    .enablePlugins(PlayScala)

  lazy val root = Project(id = "codesearch", base = file("."))
    .aggregate(core, webServer)
    .dependsOn(core, webServer)
    .settings(commonSettings)
    .settings(
      name := "Codesearch",
      assemblyJarName in assembly := "codesearch.jar",
      assemblyOutputPath in assembly := baseDirectory.value / "../codesearch.jar"
    )

}
