package com.softwaremill.loom.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class ActorRef {
    private final LinkedBlockingQueue<PendingMessage<?>> queue;

    ActorRef(LinkedBlockingQueue<PendingMessage<?>> queue) {
        this.queue = queue;
    }

    /**
     * Allows sending any message to the actor, while we should only allow sending messages that are supported by
     * the actor's behavior. However, it seems that this is not possible in Java, as it requires higher-kinded type
     * constraints or more advanced type bounds.
     */
    public <R> Future<R> send(Message<R> message) {
        var future = new CompletableFuture<R>();
        queue.add(new PendingMessage<>(message, future));
        return future;
    }
}
