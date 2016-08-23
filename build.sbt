name := "akka-udp-stream"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-actor"          % "2.4.9-RC2",
  "com.typesafe.akka"      %% "akka-stream"         % "2.4.9-RC2",
  "org.scalatest"          %% "scalatest"           % "3.0.0"       % "test",
  "com.typesafe.akka"      %% "akka-testkit"        % "2.4.9-RC2"   % "test",
  "com.typesafe.akka"      %% "akka-stream-testkit" % "2.4.9-RC2"   % "test"
)

fork in run := true
cancelable in Global :=true
