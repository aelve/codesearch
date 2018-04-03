name := "codesearch"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.7.0" ,
  "com.lihaoyi" %% "ammonite-ops" % "1.0.3",
  "org.rauschig" % "jarchivelib" % "0.7.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "io.suzaku" %% "boopickle" % "1.3.0",
  "commons-io" % "commons-io" % "2.6"
)

assemblyJarName in assembly := "codesearch.jar"
assemblyOutputPath in assembly := baseDirectory.value / "../codesearch.jar"
