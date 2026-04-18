package com.example.st_meet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "meetings_db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_MEETINGS = "meetings";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MEETING_ID = "meeting_id";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_SUMMARY = "summary";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MEETINGS_TABLE = "CREATE TABLE " + TABLE_MEETINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MEETING_ID + " TEXT,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_SUMMARY + " TEXT" + ")";
        db.execSQL(CREATE_MEETINGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEETINGS);
        onCreate(db);
    }

    public long insertMeeting(String meetingId, String createdAt, String summary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEETING_ID, meetingId);
        values.put(COLUMN_CREATED_AT, createdAt);
        values.put(COLUMN_SUMMARY, summary);
        long id = db.insert(TABLE_MEETINGS, null, values);
        db.close();
        return id;
    }

    public List<Meeting> getAllMeetings() {
        List<Meeting> meetings = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MEETINGS + " ORDER BY id DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Meeting meeting = new Meeting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEETING_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY))
                );
                meetings.add(meeting);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return meetings;
    }

    public Meeting getMeetingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEETINGS, new String[]{COLUMN_ID, COLUMN_MEETING_ID, COLUMN_CREATED_AT, COLUMN_SUMMARY},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        Meeting meeting = new Meeting(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEETING_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY))
        );
        cursor.close();
        db.close();
        return meeting;
    }

    public void deleteMeeting(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEETINGS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
