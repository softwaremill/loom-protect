package com.softwaremill.loom;

import com.softwaremill.loom.actor.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Example2 {
    record Compute(int complexity) implements Message<Long> {}

    public static void main(String[] args) throws Exception {
        var computeActor = Actor.create((ActorBehavior<Compute>) (self, message) -> {
            Thread.sleep(1000L);
            return CompletableFuture.completedFuture(message.reply(42L));
        });

        var proxyActor = Actor.create(new ActorBehavior<>() {
            @Override
            public Future<Reply> onMessage(ActorRef self, Message<?> message) {
                return handle(message);
            }

            private <R> Future<Reply> handle(Message<R> message) {
                var computeResult = computeActor.send(message);
                var reply = new CompletableFuture<Reply>();
                Thread.startVirtualThread(() -> {
                    try {
                        reply.complete(message.reply(computeResult.get()));
                    } catch (Exception e) {
                        reply.completeExceptionally(e);
                    }
                });
                return reply;
            }
        });

        var result = proxyActor.send(new Compute(10)).get();
        System.out.println("Got result: " + result);
    }
}
