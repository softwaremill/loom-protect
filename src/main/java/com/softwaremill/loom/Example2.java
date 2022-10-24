package com.softwaremill.loom;

import com.softwaremill.loom.actor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Example2 {
    private static final Logger logger = LoggerFactory.getLogger(Example2.class);

    public static void main(String[] args) throws Exception {
        var manager = createManagerActor(3, Example2::createComputeActor);

        for (int i = 0; i < 10; i++) {
            sendAndLogResult(manager, new Compute());
        }

        manager.send(new Compute()).get();
    }

    private static void sendAndLogResult(ActorRef manager, Message<?> message) {
        Thread.startVirtualThread(() -> {
            try {
                var result = manager.send(message).get();
                logger.info("Got result: " + result);
            } catch (InterruptedException e) {
                logger.info("Got interrupted");
            } catch (ExecutionException e) {
                logger.info("Execution exception", e);
            }
        });
    }

    //

    record Compute() implements Message<Long> {}

    private static ActorRef createComputeActor() {
        return Actor.create((ActorBehavior<Compute>) (self, message) -> {
            Thread.sleep(1000L);
            return CompletableFuture.completedFuture(message.reply(42L));
        });
    }

    //

    record FreeWorker(ActorRef worker) implements Message<Void> {}

    record AwaitingMessage<REPLY>(Message<REPLY> message, CompletableFuture<Reply> future) {}

    private static ActorRef createManagerActor(int count, Supplier<ActorRef> createWorker) {
        var workers = Stream.generate(createWorker).limit(count).toList();
        return Actor.create(new ActorBehavior<>() {
            private final Queue<ActorRef> freeWorkers = new LinkedList<>(workers);
            private final Queue<AwaitingMessage<?>> awaitingMessages = new LinkedList<>();

            @Override
            public Future<Reply> onMessage(ActorRef self, Message<?> message) {
                Future<Reply> reply;
                if (message instanceof FreeWorker freeWorker) {
                    freeWorkers.add(freeWorker.worker());
                    reply = CompletableFuture.completedFuture(freeWorker.reply(null));
                } else {
                    var completableReply = new CompletableFuture<Reply>();
                    awaitingMessages.add(new AwaitingMessage(message, completableReply));
                    reply = completableReply;
                }

                while (!freeWorkers.isEmpty() && !awaitingMessages.isEmpty()) {
                    handle(self, awaitingMessages.poll(), freeWorkers.poll());
                }

                return reply;
            }

            private <REPLY> void handle(ActorRef self, AwaitingMessage<REPLY> awaiting, ActorRef delegate) {
                var message = awaiting.message();
                var computeResult = delegate.send(message);
                Thread.startVirtualThread(() -> {
                    try {
                        awaiting.future().complete(message.reply(computeResult.get()));
                    } catch (Exception e) {
                        awaiting.future().completeExceptionally(e);
                    } finally {
                        self.send(new FreeWorker(delegate));
                    }
                });
            }
        });
    }
}
