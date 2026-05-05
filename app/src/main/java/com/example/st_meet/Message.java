package com.example.st_meet;

public class Message {
    private int id;
    private String meetingId;
    private String userName;
    private String messageText;
    private String timestamp;

    public Message(int id, String meetingId, String userName, String messageText, String timestamp) {
        this.id = id;
        this.meetingId = meetingId;
        this.userName = userName;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getMeetingId() { return meetingId; }
    public String getUserName() { return userName; }
    public String getMessageText() { return messageText; }
    public String getTimestamp() { return timestamp; }
}
