package com.softwaremill.loom.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Actor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Actor.class);

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
                    handleReply(pending, reply);
                } catch (Exception e) {
                    logger.error("Exception when processing: " + pending.message(), e);
                    pending.future().completeExceptionally(e);
                }
            }
        }
    }

    private void handleReply(PendingMessage<?> pending, Future<Reply> reply) {
        if (reply != null) {
            Thread.startVirtualThread(() -> {
                try {
                    //noinspection unchecked
                    ((CompletableFuture<Object>) pending.future()).complete(reply.get().getReply());
                } catch (Exception e) {
                    pending.future().completeExceptionally(e);
                }
            });
        } else pending.future().complete(null);
    }

    public static <MSG extends Message<?>> ActorRef create(ActorBehavior<MSG> behavior) {
        var queue = new LinkedBlockingQueue<PendingMessage<?>>();
        var self = new ActorRef(queue);
        var actor = new Actor(self, queue, behavior);
        Thread.startVirtualThread(actor);
        return self;
    }
}
