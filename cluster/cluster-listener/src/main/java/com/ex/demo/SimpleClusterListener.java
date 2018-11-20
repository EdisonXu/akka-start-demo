package com.ex.demo;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author edison
 * On 2018/10/25 10:28
 */
public class SimpleClusterListener extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());

    //subscribe to cluster changes


    @Override
    public void preStart() throws Exception {
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
        log.info("I'm about to start! Code: {} ", getSelf().hashCode());
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClusterEvent.MemberUp.class, mUp->log.info("Member is Up: {}", mUp.member()))
                .match(ClusterEvent.UnreachableMember.class, mUnreachable->log.info("Member detected as unreachable: {}", mUnreachable.member()))
                .match(ClusterEvent.MemberRemoved.class, mRemoved->log.info("Member is Removed: {}", mRemoved.member()))
                .match(ClusterEvent.LeaderChanged.class, msg->log.info("Leader is changed: {}", msg.getLeader()))
                .match(ClusterEvent.RoleLeaderChanged.class, msg->log.info("RoleLeader is changed: {}", msg.getLeader()))
                .match(ClusterEvent.MemberEvent.class, event->{}) //ignore
                .build();
    }
}
