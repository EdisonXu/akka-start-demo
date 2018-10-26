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
 * On 2018/10/26 10:43
 */
public class ToFindRemoteActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void preStart() throws Exception {
        log.info("ToFindRemoteActor is starting");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg->{
                    log.info("Msg received: {}", msg);
                })
                .build();
    }

    public static void main(String[] args) {
        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + 2551)
                .withFallback(ConfigFactory.load("remote.conf"));

        // Create an Akka system
        ActorSystem system = ActorSystem.create("sys", config);

        // Create an actor
        system.actorOf(Props.create(ToFindRemoteActor.class), "toFind");
    }
}
