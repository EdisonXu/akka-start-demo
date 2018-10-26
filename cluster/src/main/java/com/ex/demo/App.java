package com.ex.demo;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        if(args.length==0)
            startup(new String[] {"2551", "2552", "0"});
        else
            startup(args);
    }

    public static void startup(String[] ports){
        for(String port : ports){
            // Using input port to start multiple instances
            Config config = ConfigFactory.parseString(
                    "akka.remote.netty.tcp.port=" + port + "\n" +
                    "akka.remote.artery.canonical.port=" + port)
                    .withFallback(ConfigFactory.load());

            // Create an Akka system
            ActorSystem system = ActorSystem.create("ClusterSystem", config);

            // Create an
            system.actorOf(Props.create(SimpleClusterListener.class), "ClusterListener");
        }
    }
}
