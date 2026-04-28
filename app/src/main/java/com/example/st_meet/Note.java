package com.example.st_meet;

public class Note {
    private int id;
    private String date;
    private String note;

    public Note(int id, String date, String note) {
        this.id = id;
        this.date = date;
        this.note = note;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}
