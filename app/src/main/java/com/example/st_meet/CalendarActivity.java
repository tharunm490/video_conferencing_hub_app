package com.example.st_meet;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.st_meet.databinding.ActivityCalendarBinding;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;
    private DatabaseHelper dbHelper;
    private String selectedDate;
    private NoteAdapter adapter;
    private List<Note> noteList;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        checkNotificationPermission();

        // Initialize with today's date
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.getTime());
            loadNotes();
        });

        binding.fabAddNote.setOnClickListener(v -> showAddNoteDialog());

        loadNotes();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void loadNotes() {
        noteList = dbHelper.getNotesByDate(selectedDate);
        adapter = new NoteAdapter(noteList, (note, position) -> {
            dbHelper.deleteNote(note.getId());
            noteList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        binding.recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerNotes.setAdapter(adapter);
    }

    private void showAddNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        EditText editNote = dialogView.findViewById(R.id.editNoteText);

        new AlertDialog.Builder(this)
                .setTitle("Add Note for " + selectedDate)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String noteText = editNote.getText().toString().trim();
                    if (!TextUtils.isEmpty(noteText)) {
                        dbHelper.insertNote(selectedDate, noteText);
                        loadNotes();
                        scheduleNotification(selectedDate, noteText);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scheduleNotification(String dateStr, String noteText) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date != null && date.after(new Date())) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                // Set to 9 AM on that day
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                Intent intent = new Intent(this, ReminderReceiver.class);
                intent.putExtra("note_text", noteText);
                
                int requestCode = (int) System.currentTimeMillis();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
