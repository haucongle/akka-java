package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class StartStopActor1 extends AbstractActor {
    static Props props() {
        return Props.create(StartStopActor1.class, StartStopActor1::new);
    }

    @Override
    public void preStart() {
        System.out.println("1 started");
        getContext().actorOf(StartStopActor2.props(), "2");
    }


    @Override
    public void postStop() {
        System.out.println("1 stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("stop", s -> getContext().stop(getSelf()))
                .build();
    }
}
