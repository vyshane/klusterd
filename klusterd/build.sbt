enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

name := "klusterd"
packageSummary := "Akka cluster with Kubernetes: A sample project."
organization := "mu.node"
maintainer := "Vy-Shane Xie <shane@node.mu>"
version := "1.0"

scalaVersion := "2.11.7"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.4.1"
  Seq(
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,

    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )
}

import com.typesafe.sbt.packager.docker._
dockerRepository := Some("vyshane")
dockerExposedPorts := Seq(2551)

// Install dnsutils. This is needed to query the peer discovery service.
// We inject this step at the top of the Dockerfile so that it gets cached for subsequent Docker builds.
dockerCommands := Seq(
  Cmd("FROM", "java:latest"),
  Cmd("USER", "root"),
  ExecCmd("RUN", "apt-get", "-qq", "update"),
  ExecCmd("RUN", "apt-get", "-yq", "install", "dnsutils"),
  ExecCmd("RUN", "apt-get", "clean"),
  ExecCmd("RUN", "rm", "-rf", "/var/lib/apt/lists/*")
) ++ dockerCommands.value.filterNot {
  case Cmd("FROM", _) => true
  case _ => false
}

// Attempt to configure seed nodes via the peer discovery service
bashScriptExtraDefines += """
my_ip=$(hostname --ip-address)

SEED_NODES=$(host $PEER_DISCOVERY_SERVICE | \
    grep -v "not found" | grep -v "connection timed out" | \
    grep -v $my_ip | \
    sort | \
    head -5 | \
    awk '{print $4}' | \
    xargs | \
    sed -e 's/ /,/g')

if [ ! -z "$SEED_NODES" ]; then
    export SEED_NODES
fi
"""
