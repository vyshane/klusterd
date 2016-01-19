# klusterd

klusterd is a sample [Akka Cluster](http://doc.akka.io/docs/akka/2.4.1/common/cluster.html) project that is packaged using [Docker](https://www.docker.com/what-docker) and deployed to [Kubernetes](http://kubernetes.io).

## Taking klusterd for a test drive

First, we'll use [sbt](http://www.scala-sbt.org) to package the application as a Docker image.

```text
$ cd klusterd
$ sbt "docker:publishLocal"
```

This creates a local Docker image for us:

```text
$ docker images | grep klusterd
vyshane/klusterd                      1.0                  43ee995e8adb        1 minute ago      690.7 MB
```

To deploy our klusterd image to our Kubernetes cluster:

```text
$ cd ../deployment
$ ./up.sh
```

We can see that a klusterd pod comes up:

```text
$ kubectl get pods
NAME                   READY     STATUS    RESTARTS   AGE
klusterd-6srxl         1/1       Running   0          1m
```

Let's tail its log:

```text
$ kubectl logs -f klusterd-6srxl
```

We can see that klusterd is running at the IP address 172.17.0.3 and listening on port 2551.

```text
INFO  14:58:06.022UTC akka.remote.Remoting - Starting remoting
INFO  14:58:06.235UTC akka.remote.Remoting - Remoting started; listening on addresses :[akka.tcp://klusterd@172.17.0.3:2551]
```

Since we have only launched one klusterd node, it is its own cluster seed node.

```text
INFO  14:58:06.446UTC akka.actor.ActorSystemImpl(klusterd) - Configured seed nodes: akka.tcp://klusterd@172.17.0.3:2551
```

We have a cluster of one.

```text
INFO  14:58:06.454UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Node [akka.tcp://klusterd@172.17.0.3:2551] is JOINING, roles []
INFO  14:58:06.482UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Leader is moving node [akka.tcp://klusterd@172.17.0.3:2551] to [Up]
INFO  14:58:06.505UTC akka.tcp://klusterd@172.17.0.3:2551/user/cluster-monitor - Cluster member up: akka.tcp://klusterd@172.17.0.3:2551
```

Now, let's scale this cluster up. Let's ask Kubernetes for 2 more nodes:

```text
$ kubectl scale --replicas=3 rc klusterd
```

We can see from `klusterd-6srxl`'s logs that two more nodes join our cluster shortly after.

```text
INFO  15:11:46.525UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Node [akka.tcp://klusterd@172.17.0.4:2551] is JOINING, roles []
INFO  15:11:46.533UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Node [akka.tcp://klusterd@172.17.0.5:2551] is JOINING, roles []
INFO  15:11:47.484UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Leader is moving node [akka.tcp://klusterd@172.17.0.4:2551] to [Up]
INFO  15:11:47.505UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.3:2551] - Leader is moving node [akka.tcp://klusterd@172.17.0.5:2551] to [Up]
INFO  15:11:47.507UTC akka.tcp://klusterd@172.17.0.3:2551/user/cluster-monitor - Cluster member up: akka.tcp://klusterd@172.17.0.4:2551
INFO  15:11:47.507UTC akka.tcp://klusterd@172.17.0.3:2551/user/cluster-monitor - Cluster member up: akka.tcp://klusterd@172.17.0.5:2551
```

We can scale the cluster down:

```text
$ kubectl scale --replicas=2 rc klusterd
```

And a node leaves the cluster. In our case, the original pod `klusterd-6srxl` was killed. Here are the logs from another node showing what happened:

```text
WARN  15:19:31.855UTC akka.tcp://klusterd@172.17.0.4:2551/system/endpointManager/reliableEndpointWriter-akka.tcp%3A%2F%2Fklusterd%40172.17.0.3%3A2551-0 - Association with remote system [akka.tcp://klusterd@172.17.0.3:2551] has failed, address is now gated for [5000] ms. Reason: [Disassociated] 
WARN  15:19:35.376UTC akka.tcp://klusterd@172.17.0.4:2551/user/cluster-monitor - Cluster member unreachable: akka.tcp://klusterd@172.17.0.3:2551
WARN  15:19:35.405UTC akka.tcp://klusterd@172.17.0.4:2551/system/cluster/core/daemon - Cluster Node [akka.tcp://klusterd@172.17.0.4:2551] - Marking node(s) as UNREACHABLE [Member(address = akka.tcp://klusterd@172.17.0.3:2551, status = Up)]
INFO  15:19:45.384UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.4:2551] - Leader is auto-downing unreachable node [akka.tcp://klusterd@172.17.0.3:2551]
INFO  15:19:45.387UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.4:2551] - Marking unreachable node [akka.tcp://klusterd@172.17.0.3:2551] as [Down]
INFO  15:19:46.412UTC akka.cluster.Cluster(akka://klusterd) - Cluster Node [akka.tcp://klusterd@172.17.0.4:2551] - Leader is removing unreachable node [akka.tcp://klusterd@172.17.0.3:2551]
INFO  15:19:46.414UTC akka.tcp://klusterd@172.17.0.4:2551/user/cluster-monitor - Cluster member removed: akka.tcp://klusterd@172.17.0.3:2551
WARN  15:19:52.756UTC akka.tcp://klusterd@172.17.0.4:2551/system/endpointManager/reliableEndpointWriter-akka.tcp%3A%2F%2Fklusterd%40172.17.0.3%3A2551-3 - Association with remote system [akka.tcp://klusterd@172.17.0.3:2551] has failed, address is now gated for [5000] ms. Reason: [Association failed with [akka.tcp://klusterd@172.17.0.3:2551]] Caused by: [No response from remote for outbound association. Associate timed out after [15000 ms].]
INFO  15:19:52.758UTC akka.tcp://klusterd@172.17.0.4:2551/system/transports/akkaprotocolmanager.tcp0/akkaProtocol-tcp%3A%2F%2Fklusterd%40172.17.0.3%3A2551-4 - No response from remote for outbound association. Associate timed out after [15000 ms].
```

To turn off everything:

```text
./down.sh
```
