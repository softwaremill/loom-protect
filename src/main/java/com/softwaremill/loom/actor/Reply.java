package com.softwaremill.loom.actor;

public class Reply {
    private final Object reply;

    Reply(Object reply) {
        this.reply = reply;
    }

    Object getReply() {
        return reply;
    }
}
