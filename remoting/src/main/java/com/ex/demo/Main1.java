package com.ex.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

/**
 * Try send message from local actor (with local provider and dispatcher) to a remote actor.
 * It will fail because remote actor in Akka is designed to be Peer-to-Peer, requiring both equally to be remote actor.
 *
 * @author edison
 * On 2018/10/26 10:30
 */
public class Main1
{
    public static void main( String[] args )
    {
        ActorSystem system = ActorSystem.create("main1");
        ActorSelection toFind = system.actorSelection("akka.tcp://sys@127.0.0.1:2551/user/toFind");
        toFind.tell("hello", ActorRef.noSender());
    }
}
