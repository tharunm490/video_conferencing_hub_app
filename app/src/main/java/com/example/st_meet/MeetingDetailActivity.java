package com.example.st_meet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.st_meet.databinding.ActivityMeetingDetailBinding;

public class MeetingDetailActivity extends AppCompatActivity {

    private ActivityMeetingDetailBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeetingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        int dbId = getIntent().getIntExtra("meeting_id_db", -1);
        if (dbId != -1) {
            loadMeetingDetail(dbId);
        }
    }

    private void loadMeetingDetail(int id) {
        Meeting meeting = dbHelper.getMeetingById(id);
        if (meeting != null) {
            binding.textMeetingId.setText(meeting.getMeetingId());
            binding.textCreatedAt.setText(meeting.getCreatedAt());
            binding.textFullSummary.setText(meeting.getSummary());
            binding.textFullTranscript.setText(meeting.getFullText());
        }
    }
}
