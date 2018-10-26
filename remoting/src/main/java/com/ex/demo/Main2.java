package com.ex.demo;

/**
 * Try to query remote actor
 *
 * @author edison
 * On 2018/10/26 10:30
 */

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main2 {

    public static void main(String[] args) {
        Config config = ConfigFactory.load("remote.conf");
        // Create an Akka system
        ActorSystem system = ActorSystem.create("main2", config);

        // Find remote actor
        ActorSelection toFind = system.actorSelection("akka.tcp://sys@127.0.0.1:2551/user/toFind");
        toFind.tell("hello", ActorRef.noSender());
    }
}
