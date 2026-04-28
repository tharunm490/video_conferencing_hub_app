package com.example.st_meet;

public class ChatMessage {
    private String senderName;
    private String message;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String senderName, String message, long timestamp) {
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}