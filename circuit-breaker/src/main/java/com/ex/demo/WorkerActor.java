package com.ex.demo;

import akka.actor.AbstractActor;
import akka.pattern.CircuitBreaker;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.PatternsCS.pipe;

/**
 * @author edison
 * On 2018/11/21 9:25
 */
public class WorkerActor extends AbstractActor {

    private CircuitBreaker circuitBreaker;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        this.circuitBreaker = new CircuitBreaker(getContext().dispatcher(),
                getContext().system().scheduler(), 5,
                Duration.ofSeconds(2),
                Duration.ofSeconds(10))
                .addOnOpenListener(this::onOpen)
                .addOnHalfOpenListener(this::onHalfOpen)
                .addOnCloseListener(this::onClose);
    }

    public void onOpen(){
        System.out.println("CircuitBreaker is open");
    }

    public void onHalfOpen(){
        System.out.println("CircuitBreaker is half-open");
    }

    public void onClose(){
        System.out.println("CircuitBreaker is close");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, "async"::equals, str-> pipe(
                    circuitBreaker.callWithCircuitBreakerCS(() ->
                            CompletableFuture.supplyAsync(this::dangerousCall)
                ), getContext().dispatcher()
                ).to(sender()))
                .match(String.class, "sync"::equals, str->
                        sender().tell(circuitBreaker.callWithSyncCircuitBreaker(this::dangerousCall), self())
                )
                .build();
    }

    public String dangerousCall(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Simulate unstable actor";
    }
}
