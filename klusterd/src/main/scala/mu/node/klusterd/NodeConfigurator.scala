package mu.node.klusterd

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

object NodeConfigurator {

  lazy val config = loadConfig()

  def loadConfig(): Config = {

    def getHostLocalAddress: Option[String] = {
      import java.net.NetworkInterface

      import scala.collection.JavaConversions._

      NetworkInterface.getNetworkInterfaces
        .find(_.getName equals "eth0")
        .flatMap { interface =>
          interface.getInetAddresses.find(_.isSiteLocalAddress).map(_.getHostAddress)
        }
    }

    def getSeedNodes(config: Config): Array[String] = {
      if (config.hasPath("klusterd.seed-nodes")) {
        config.getString("klusterd.seed-nodes").split(",").map(_.trim)
      } else {
        Array.empty
      }
    }

    def formatSeedNodesConfig(clusterName: String, seedNodeAddresses: Array[String], seedNodePort: String,
                              defaultSeedNodeAddress: String): String = {
      if (seedNodeAddresses.length > 0) {
        seedNodeAddresses.map { address =>
          s"""akka.cluster.seed-nodes += "akka.tcp://$clusterName@$address:$seedNodePort""""
        }.mkString("\n")
      } else {
        s"""akka.cluster.seed-nodes = ["akka.tcp://$clusterName@$defaultSeedNodeAddress:$seedNodePort"]"""
      }
    }

    val config = ConfigFactory.load()
    val clusterName = config.getString("klusterd.cluster-name")
    val seedPort = config.getString("klusterd.seed-port")

    val host = if (config.getString("klusterd.host") == "eth0-address-or-localhost") {
      getHostLocalAddress.getOrElse("127.0.0.1")
    } else {
      config.getString("klusterd.host")
    }

    ConfigFactory.parseString(formatSeedNodesConfig(clusterName, getSeedNodes(config), seedPort, host))
      .withValue("klusterd.host", ConfigValueFactory.fromAnyRef(host))
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(host))
      .withFallback(config)
      .resolve()
  }
}
