package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

public class AkkaQuickstart {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("testSystem");

        ActorRef firstRef = system.actorOf(PrintMyActorRefActor.props(), "first-actor");
        System.out.println("1-" + firstRef);
        firstRef.tell("printit", ActorRef.noSender());

        System.out.println(">>> Press ENTER to exit <<<");
        try {
            System.in.read();
        } catch (IOException ignored) {
        } finally {
            system.terminate();
        }
    }
}
