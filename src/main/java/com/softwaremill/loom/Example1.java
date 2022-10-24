package com.softwaremill.loom;

import com.softwaremill.loom.actor.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Example1 {
    sealed interface CounterMessage<R> extends Message<R> {}

    record Increase(int i) implements CounterMessage<Void> {}

    record Decrease(int i) implements CounterMessage<Void> {}

    record Get() implements CounterMessage<Integer> {}

    static class CounterActorBehavior implements ActorBehavior<CounterMessage<?>> {
        int counter = 0;

        @Override
        public Future<Reply> onMessage(ActorRef self, CounterMessage<?> message) {
            Reply reply = null;
            switch (message) {
                case Increase(int i) inc -> {
                    System.out.println("Got an increase message, by: " + i);
                    counter += i;
                    reply = inc.reply(null);
                }
                case Decrease(int i) dec -> {
                    System.out.println("Got a decrease message, by: " + i);
                    counter -= i;
                    reply = dec.reply(null);
                }
                case Get() get -> {
                    System.out.println("Got a get message, current state: " + counter);
                    reply = get.reply(counter);
                }
            }
            return CompletableFuture.completedFuture(reply);
        }
    }

    public static void main(String[] args) throws Exception {
        var actor = Actor.create(new CounterActorBehavior());

        actor.send(new Increase(10));
        actor.send(new Decrease(8));
        var result = actor.send(new Get()).get();
        System.out.println("Got result: " + result);
    }
}
