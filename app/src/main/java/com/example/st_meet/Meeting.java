package com.example.st_meet;

public class Meeting {
    private int id;
    private String meetingId;
    private String createdAt;
    private String summary;

    public Meeting(int id, String meetingId, String createdAt, String summary) {
        this.id = id;
        this.meetingId = meetingId;
        this.createdAt = createdAt;
        this.summary = summary;
    }

    public Meeting(String meetingId, String createdAt, String summary) {
        this.meetingId = meetingId;
        this.createdAt = createdAt;
        this.summary = summary;
    }

    public int getId() { return id; }
    public String getMeetingId() { return meetingId; }
    public String getCreatedAt() { return createdAt; }
    public String getSummary() { return summary; }
}
