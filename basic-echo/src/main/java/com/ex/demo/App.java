package com.ex.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import scala.concurrent.Future;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        //akka.Main.main(new String[]{EchoActor.class.getName()});
        ActorSystem system = ActorSystem.create("app");
        ActorRef echoActor = system.actorOf(Props.create(EchoActor.class), "echoActor");
        echoActor.tell("hello", ActorRef.noSender());
        echoActor.tell(1, ActorRef.noSender());
        Future<Object> future = Patterns.ask(echoActor, "echo me", 200);
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object result) throws Throwable {
                System.out.println(result);
            }
        }, system.dispatcher());
    }
}
