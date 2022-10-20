package com.softwaremill.loom.actor;

import java.util.concurrent.Future;

public interface ActorBehavior<MSG extends Message<?>> {
    Future<Reply> onMessage(ActorRef self, MSG message) throws Exception;
}


