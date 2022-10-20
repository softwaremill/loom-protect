package com.softwaremill.loom.actor;

public class Reply<R> {
    private final R reply;

    Reply(R reply) {
        this.reply = reply;
    }

    R getReply() {
        return reply;
    }
}
