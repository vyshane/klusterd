package mu.node.klusterd

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

class ClusterMonitor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) => log.info(s"Cluster member up: ${member.address}")
    case UnreachableMember(member) => log.warning(s"Cluster member unreachable: ${member.address}")
    case MemberRemoved(member, previousStatus) => log.info(s"Cluster member removed: ${member.address}")
    case MemberExited(member) => log.info(s"Cluster member exited: ${member.address}")
    case _: MemberEvent =>
  }
}