package com.softwaremill.loom.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Actor implements Runnable {
    private final ActorRef self;

    private final LinkedBlockingQueue<PendingMessage<?>> queue;

    private final ActorBehavior<Message<?>> behavior;

    Actor(ActorRef self, LinkedBlockingQueue<PendingMessage<?>> queue, ActorBehavior<?> behavior) {
        this.self = self;
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
                    var reply = behavior.onMessage(self, pending.message());
                    if (reply != null) {
                        PendingMessage<?> finalPending = pending;
                        Thread.startVirtualThread(() -> {
                            try {
                                //noinspection unchecked
                                ((CompletableFuture<Object>) finalPending.future()).complete(reply.get().getReply());
                            } catch (Exception e) {
                                finalPending.future().completeExceptionally(e);
                            }
                        });
                    } else pending.future().complete(null);
                } catch (Exception e) {
                    pending.future().completeExceptionally(e);
                }
            }
        }
    }

    public static <MSG extends Message<?>> ActorRef create(ActorBehavior<MSG> behavior) {
        var queue = new LinkedBlockingQueue<PendingMessage<?>>();
        var self = new ActorRef(queue);
        var actor = new Actor(self, queue, behavior);
        Thread.startVirtualThread(actor);
        return self;
    }
}
