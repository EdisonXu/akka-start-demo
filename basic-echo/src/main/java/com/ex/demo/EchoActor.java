package com.ex.demo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author edison
 * On 2018/10/29 14:28
 */
public class EchoActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    log.info("Received String message: {}", s);
                    ActorRef sender = getSender();
                    if(!sender.isTerminated())
                        sender.tell("Receive: "+s, getSelf());
                })
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

}
