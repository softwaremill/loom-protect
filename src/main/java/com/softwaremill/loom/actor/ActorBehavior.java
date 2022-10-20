package com.softwaremill.loom.actor;

public interface ActorBehavior<MSG extends Message<?>> {
    Reply<?> onMessage(MSG message);
}


