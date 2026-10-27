package com.example.astra;

public class Message {
    private final String text;
    private final boolean user;
    private final boolean typing;
    private final long timestamp;

    public Message(String text, boolean user, long timestamp) {
        this(text, user, false, timestamp);
    }

    private Message(String text, boolean user, boolean typing, long timestamp) {
        this.text = text;
        this.user = user;
        this.typing = typing;
        this.timestamp = timestamp;
    }

    public static Message typing() {
        return new Message("ASTRA is thinking...", false, true, System.currentTimeMillis());
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return user;
    }

    public boolean isTyping() {
        return typing;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
