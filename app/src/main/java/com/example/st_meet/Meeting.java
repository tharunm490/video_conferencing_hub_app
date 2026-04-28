package com.example.st_meet;

public class Meeting {
    private int id;
    private String meetingId;
    private String createdAt;
    private String summary;
    private String fullText;

    public Meeting(int id, String meetingId, String createdAt, String summary, String fullText) {
        this.id = id;
        this.meetingId = meetingId;
        this.createdAt = createdAt;
        this.summary = summary;
        this.fullText = fullText;
    }

    public Meeting(String meetingId, String createdAt, String summary, String fullText) {
        this.meetingId = meetingId;
        this.createdAt = createdAt;
        this.summary = summary;
        this.fullText = fullText;
    }

    public int getId() { return id; }
    public String getMeetingId() { return meetingId; }
    public String getCreatedAt() { return createdAt; }
    public String getSummary() { return summary; }
    public String getFullText() { return fullText; }
    public String getTranscript() { return fullText; }
}
