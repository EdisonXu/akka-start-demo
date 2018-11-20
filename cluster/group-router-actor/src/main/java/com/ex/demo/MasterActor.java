package com.ex.demo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.routing.ClusterRouterGroup;
import akka.cluster.routing.ClusterRouterGroupSettings;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.management.AkkaManagement;
import akka.management.cluster.ClusterHttpManagement;
import akka.routing.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author edison
 * On 2018/11/16 9:05
 */
public class MasterActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private ActorRef router;

    @Override
    public void preStart() throws Exception {
        router = getContext().actorOf(FromConfig.getInstance().props(Props.create(SlaveActor.class)), "groupRouter");
        /*List<String> routeesPaths = Arrays.asList("akka/user/slaveActor");
        router = getContext().actorOf(new RoundRobinGroup(routeesPaths).props(), "groupRouter");*/
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
        /*system.actorOf(Props.create(SlaveActor.class), "slaveActor1");
        system.actorOf(Props.create(SlaveActor.class), "slaveActor2");
        system.actorOf(Props.create(SlaveActor.class), "slaveActor3");*/
    }
}
