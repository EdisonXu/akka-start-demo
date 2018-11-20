package com.ex.demo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.management.AkkaManagement;
import akka.management.cluster.ClusterHttpManagement;
import akka.routing.FromConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author edison
 * On 2018/11/16 9:05
 */
public class MasterActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private ActorRef router;

    @Override
    public void preStart() throws Exception {
        router = getContext().actorOf(FromConfig.getInstance().props(Props.create(SlaveActor.class)), "poolRouter");
        /*int totalInstances = 1000;
        int maxInstancePerNode = 5, routeeNumbers=5;
        boolean allowLocalRoutees = true;
        String role = "master";
        ClusterRouterPoolSettings settings = new ClusterRouterPoolSettings(totalInstances, maxInstancePerNode, allowLocalRoutees, role);
        ClusterRouterPool routerPool = new ClusterRouterPool(new RoundRobinPool(routeeNumbers), settings);
        router = getContext().actorOf(routerPool.props(Props.create(SlaveActor.class)), "poolRouter");*/
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg->{
                    log.info("Master got: {}", msg);
                    router.tell(msg, getSender());
                })
                .build();
    }

    public static void main(String[] args) {
        int port = 2551;

        // Override the configuration of the port
        Config config =
                ConfigFactory.parseString(
                        "akka.remote.netty.tcp.port=" + port + "\n" +
                                "akka.remote.artery.canonical.port=" + port)
                        .withFallback(
                                ConfigFactory.parseString("akka.cluster.roles = [master]"))
                        .withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        ClusterHttpManagement.get(system);
        AkkaManagement.get(system).start();
        system.actorOf(Props.create(MasterActor.class), "masterActor");
    }
}
