import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.web.Import._
import play.sbt.PlayImport._
import play.sbt.PlayScala
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys.{assemblyMergeStrategy, _}
import sbtassembly.AssemblyPlugin.autoImport.{assemblyJarName, assemblyOutputPath}
import sbtassembly._

object Builder {
  lazy val commonSettings = Seq(
    organization := "org.aelve",
    version := "0.1",
    scalaVersion := "2.12.4",
    resolvers += Resolver.sbtPluginRepo("releases"),
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
      "-Xexperimental",
      "-Ypartial-unification"
    ),
    scalacOptions in (Compile, console) -= "-Ywarn-unused-import",
    scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits"),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    addCompilerPlugin("org.scalamacros" % "paradise"            % "2.1.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.0-M4"),
    addCompilerPlugin("org.spire-math"  %% "kind-projector"     % "0.9.8")
  )

  lazy val commonDeps = Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi"          %% "fastparse"      % "2.0.4",
      "com.github.scopt"     %% "scopt"          % "3.7.0",
      "com.lihaoyi"          %% "ammonite-ops"   % "1.0.3",
      "org.rauschig"         % "jarchivelib"     % "0.7.1",
      "commons-io"           % "commons-io"      % "2.6",
      "javax.inject"         % "javax.inject"    % "1",
      "ch.qos.logback"       % "logback-classic" % "1.2.3",
      "org.codehaus.janino"  % "janino"          % "3.0.11",
      "com.typesafe.play"    %% "play-json"      % "2.6.9",
      "com.github.mpilquist" %% "simulacrum"     % "0.13.0",
      "org.typelevel"        %% "cats-core"      % "1.2.0"
    )
  )

  lazy val core = Project(id = "core", base = file("core"))
    .settings(commonSettings ++ commonDeps)
    .settings(name := "codesearch-core")
    .settings(
      assemblyJarName in assembly := "codesearch-core.jar",
      assemblyOutputPath in assembly := baseDirectory.value / "../codesearch-core.jar",
      libraryDependencies ++= Seq(
        "org.testcontainers"    % "postgresql"                     % "1.11.1",
        "com.dimafeng"          %% "testcontainers-scala"          % "0.23.0" % "test",
        "com.typesafe.slick"    %% "slick"                         % "3.2.3",
        "com.typesafe.slick"    %% "slick-hikaricp"                % "3.2.3",
        "org.postgresql"        % "postgresql"                     % "42.2.2",
        "com.softwaremill.sttp" %% "async-http-client-backend-fs2" % "1.3.8",
        "co.fs2"                %% "fs2-core"                      % "1.0.0",
        "co.fs2"                %% "fs2-io"                        % "1.0.0",
        "io.circe"              %% "circe-fs2"                     % "0.10.0",
        "io.circe"              %% "circe-core"                    % "0.10.0",
        "io.circe"              %% "circe-generic"                 % "0.10.0",
        "io.circe"              %% "circe-parser"                  % "0.10.0",
        "com.github.derekjw"    %% "fs2json-core"                  % "0.8.0",
        "com.github.pureconfig" %% "pureconfig"                    % "0.9.2",
        "com.github.pureconfig" %% "pureconfig-cats-effect"        % "0.9.2",
        "io.chrisdavenport"     %% "log4cats-slf4j"                % "0.2.0-RC2",
        "org.scalactic"         %% "scalactic"                     % "3.0.5",
        "org.scalatest"         %% "scalatest"                     % "3.0.5" % "test"
      ),
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", _ @_*) => MergeStrategy.discard
        case PathList("reference.conf")  => MergeStrategy.concat
        case _                           => MergeStrategy.first
      }
    )

  lazy val webServer = Project(id = "web-server", base = file("web-server"))
    .settings(commonSettings ++ commonDeps)
    .settings(
      name := "codesearch-web-server",
      libraryDependencies ++= Seq(
        guice,
        "org.webjars"          % "bootstrap" % "4.1.0",
        "com.github.marlonlom" % "timeago"   % "3.0.2"
      ),
      fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
        case manifest if manifest.contains("MANIFEST.MF")         =>
          // We don't need manifest files since sbt-assembly will create
          // one with the given settings
          MergeStrategy.discard
        case PathList("org", "scalatools", "testing", xs @ _*) =>
          MergeStrategy.first
        case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
          MergeStrategy.concat
        case "application.conf" | "production.conf" => MergeStrategy.concat
        case "logback.xml"                          => MergeStrategy.first
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := "codesearch-server.jar",
      assemblyOutputPath in assembly := baseDirectory.value / "../codesearch-server.jar",
      pipelineStages := Seq(digest),
      pipelineStages in Assets := Seq(digest)
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
