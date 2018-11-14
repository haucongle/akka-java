package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Printer.Greeting;

public class Greeter extends AbstractActor {
    static Props props(String message, ActorRef printerActor) {
        return Props.create(Greeter.class, () -> new Greeter(message, printerActor));
    }

    static class WhoToGreet {
        final String who;

        WhoToGreet(String who) {
            this.who = who;
        }
    }

    static class Greet {
    }

    private final String message;
    private final ActorRef printerActor;
    private String greeting = "";

    private Greeter(String message, ActorRef printerActor) {
        this.message = message;
        this.printerActor = printerActor;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WhoToGreet.class, wtg -> this.greeting = message + ", " + wtg.who)
                .match(Greet.class, x -> printerActor.tell(new Greeting(greeting), getSelf()))
                .build();
    }
}
