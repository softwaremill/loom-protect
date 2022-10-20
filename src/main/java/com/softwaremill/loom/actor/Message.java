package com.softwaremill.loom.actor;

public interface Message<REPLY> {
    default Reply reply(REPLY r) {
        return new Reply(r);
    }
}
