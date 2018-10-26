package com.ex.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Depends on Main2. Need to start Main2 first, which will start a remote actor system listened on 127.0.0.1:2552.
 * Then try to create a ToCreateRemoteActor on that remote system.
 *
 * @author edison
 * On 2018/10/26 11:35
 */
public class Main3 {

    public static void main(String[] args) {
        Config config = ConfigFactory.load("create_remote.conf");
        // Create an Akka system
        ActorSystem system = ActorSystem.create("main3", config);

        // Create an remote actor on target path
        ActorRef actor = system.actorOf(Props.create(ToCreateRemoteActor.class), "toCreateActor");
        actor.tell("I'm created!", ActorRef.noSender());
    }

}
