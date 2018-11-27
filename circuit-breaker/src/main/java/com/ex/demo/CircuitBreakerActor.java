package com.ex.demo;

import akka.actor.*;
import akka.actor.SupervisorStrategy.Directive;
import akka.dispatch.OnSuccess;
import akka.japi.function.Function;
import akka.japi.pf.DeciderBuilder;
import akka.pattern.CircuitBreaker;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.time.Duration;

/**
 * Hello world!
 *
 */
public class CircuitBreakerActor extends AbstractActor
{

    private ActorRef workerRef;

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(20, Duration.ofMinutes(1),
                    DeciderBuilder
                        .match(Throwable.class, t-> SupervisorStrategy.resume())
                        .build());

    @Override
    public void preStart() throws Exception {
        super.preStart();
        workerRef = getContext().actorOf(Props.create(WorkerActor.class), "workerActor");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, str->{
                    System.out.println("Forward message "+str);
                    workerRef.tell(str, getSender());
                })
                .build();
    }

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("app");
        ActorRef echoActor = system.actorOf(Props.create(CircuitBreakerActor.class), "echoActor");
        /*Future<Object> future = Patterns.ask(echoActor, "async", 5000);
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object result) throws Throwable {
                System.out.println(result);
            }
        }, system.dispatcher());*/
        while(true){
            Thread.sleep(1000);
            echoActor.tell("async", ActorRef.noSender());
        }
    }
}
