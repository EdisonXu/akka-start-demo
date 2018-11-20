package com.ex.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.management.AkkaManagement;
import akka.management.cluster.ClusterHttpManagement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * This is a sample that wrap
 *
 */
public class Client
{
    public static void main( String[] args ) throws InterruptedException {
        Config config = ConfigFactory.load();
        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        ActorSelection toFind = system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/masterActor");
        int counter = 0;
        while(true){
            toFind.tell("hello "+counter++, ActorRef.noSender());
            System.out.println("Finish telling");
            Thread.sleep(2000);
        }
    }
}
