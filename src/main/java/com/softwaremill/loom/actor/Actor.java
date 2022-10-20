package com.softwaremill.loom.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Actor implements Runnable {
    private final LinkedBlockingQueue<PendingMessage<?>> queue;

    private final ActorBehavior<Message<?>> behavior;

    Actor(LinkedBlockingQueue<PendingMessage<?>> queue, ActorBehavior<?> behavior) {
        this.queue = queue;
        // see comment in ActorRef
        //noinspection unchecked
        this.behavior = (ActorBehavior<Message<?>>) behavior;
    }

    public void run() {
        var running = true;
        while (running) {
            PendingMessage<?> pending = null;
            try {
                pending = queue.poll(1000, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                running = false;
            }

            if (pending != null) {
                try {
                    var reply = behavior.onMessage(pending.message());
                    if (reply != null) {
                        //noinspection unchecked
                        ((CompletableFuture<Object>) pending.future()).complete(reply.getReply());
                    }
                } catch (Exception e) {
                    pending.future().completeExceptionally(e);
                }
            }
        }
    }

    public static <MSG extends Message<?>> ActorRef create(ActorBehavior<MSG> behavior) {
        var queue = new LinkedBlockingQueue<PendingMessage<?>>();
        var actor = new Actor(queue, behavior);
        Thread.startVirtualThread(actor);
        return new ActorRef(queue);
    }
}
