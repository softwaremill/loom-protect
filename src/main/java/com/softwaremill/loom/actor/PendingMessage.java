package com.softwaremill.loom.actor;

import java.util.concurrent.CompletableFuture;

record PendingMessage<R>(Message<R> message, CompletableFuture<R> future) {}
