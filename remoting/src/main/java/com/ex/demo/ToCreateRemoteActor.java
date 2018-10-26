package com.ex.demo;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author edison
 * On 2018/10/26 10:43
 */
public class ToCreateRemoteActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void preStart() throws Exception {
        log.info("ToCreateRemoteActor is starting");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg->{
                    log.info("Msg received: {}", msg);
                })
                .build();
    }
}
