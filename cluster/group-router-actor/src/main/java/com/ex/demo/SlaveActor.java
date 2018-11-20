package com.ex.demo;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author edison
 * On 2018/11/14 16:16
 */
public class SlaveActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void preStart() throws Exception {
        log.info("SlaveActor {} is started", getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, word-> log.info("Node {} receives: {}", getSelf().path().toSerializationFormat(), word))
                .build();
    }

    public static void main(String[] args) {
        Config config =
                ConfigFactory.parseString("akka.cluster.roles = [slave]")
                        .withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.actorOf(Props.create(SlaveActor.class), "slaveActor");
    }
}
