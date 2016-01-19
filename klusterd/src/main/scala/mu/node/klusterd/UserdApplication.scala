package mu.node.klusterd

import akka.actor.{ActorSystem, Props}

import scala.collection.JavaConversions._

object KlusterdApplication {

  def main(args: Array[String]): Unit = {
    val config = NodeConfigurator.config
    val system = ActorSystem(config.getString("klusterd.cluster-name"), config)
    system.log.info("Configured seed nodes: " + config.getStringList("akka.cluster.seed-nodes").mkString(", "))
    system.actorOf(Props[ClusterMonitor], "cluster-monitor")
  }
}