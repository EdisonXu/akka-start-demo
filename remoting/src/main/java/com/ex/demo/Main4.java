package com.ex.demo;

/**
 * Try to query remote actor created by Main3.
 *
 * @author edison
 * On 2018/10/26 10:30
 */

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main4 {

    public static void main(String[] args) {
        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + 0)
                .withFallback(ConfigFactory.load("remote.conf"));
        // Create an Akka system
        ActorSystem system = ActorSystem.create("main4", config);

        // Find remote actor
        ActorSelection toFind = system.actorSelection("akka.tcp://main3@127.0.0.1:2553/user/toCreateActor");
        toFind.tell("I'm alive!", ActorRef.noSender());
    }
}
