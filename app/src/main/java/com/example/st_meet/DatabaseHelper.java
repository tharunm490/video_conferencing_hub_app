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
    private static final int DATABASE_VERSION = 6;

    private static final String TABLE_MEETINGS = "meetings";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MEETING_ID = "meeting_id";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_SUMMARY = "summary";
    private static final String COLUMN_FULL_TEXT = "full_text";

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";

    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_NOTE_ID = "id";
    private static final String COLUMN_NOTE_DATE = "date";
    private static final String COLUMN_NOTE_TEXT = "note";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MEETINGS_TABLE = "CREATE TABLE " + TABLE_MEETINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MEETING_ID + " TEXT,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_SUMMARY + " TEXT,"
                + COLUMN_FULL_TEXT + " TEXT" + ")";
        db.execSQL(CREATE_MEETINGS_TABLE);

        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_USERNAME + " TEXT,"
                + COLUMN_EMAIL + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NOTE_DATE + " TEXT,"
                + COLUMN_NOTE_TEXT + " TEXT" + ")";
        db.execSQL(CREATE_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_MEETINGS + " ADD COLUMN " + COLUMN_FULL_TEXT + " TEXT");
        }
        if (oldVersion < 5) {
            String CREATE_NOTES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTES + "("
                    + COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NOTE_DATE + " TEXT,"
                    + COLUMN_NOTE_TEXT + " TEXT" + ")";
            db.execSQL(CREATE_NOTES_TABLE);
        }
        if (oldVersion < 6) {
            // Check if full_text already exists to avoid errors, although we are technically mapping it to transcript
            // In the current schema, full_text was added in version 4.
            // We'll treat COLUMN_FULL_TEXT as the transcript column.
        }
    }

    public boolean insertUser(String name, String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_EMAIL + " = ?" + " AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public boolean isUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public android.util.Pair<String, String> getUserInfo(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_NAME, COLUMN_USERNAME},
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String username = cursor.getString(1);
            cursor.close();
            return new android.util.Pair<>(name, username);
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public long insertMeeting(String meetingId, String createdAt, String summary, String fullText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEETING_ID, meetingId);
        values.put(COLUMN_CREATED_AT, createdAt);
        values.put(COLUMN_SUMMARY, summary);
        values.put(COLUMN_FULL_TEXT, fullText);
        long id = db.insert(TABLE_MEETINGS, null, values);
        // db.close(); // Commented out to keep connection open for Database Inspector
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
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_TEXT))
                );
                meetings.add(meeting);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // db.close(); // Commented out to keep connection open for Database Inspector
        return meetings;
    }

    public Meeting getMeetingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEETINGS, new String[]{COLUMN_ID, COLUMN_MEETING_ID, COLUMN_CREATED_AT, COLUMN_SUMMARY, COLUMN_FULL_TEXT},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        Meeting meeting = new Meeting(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEETING_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_TEXT))
        );
        cursor.close();
        // db.close(); // Commented out to keep connection open for Database Inspector
        return meeting;
    }

    public boolean updateUser(String email, String newName, String newUsername, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        values.put(COLUMN_USERNAME, newUsername);
        values.put(COLUMN_PASSWORD, newPassword);
        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return result > 0;
    }

    public long insertNote(String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_DATE, date);
        values.put(COLUMN_NOTE_TEXT, note);
        return db.insert(TABLE_NOTES, null, values);
    }

    public List<Note> getNotesByDate(String date) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, null, COLUMN_NOTE_DATE + "=?", new String[]{date}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                notes.add(new Note(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TEXT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteMeeting(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEETINGS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        // db.close(); // Commented out to keep connection open for Database Inspector
    }
}
