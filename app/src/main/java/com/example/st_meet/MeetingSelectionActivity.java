package com.example.st_meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.st_meet.databinding.ActivityMeetingSelectionBinding;

import java.util.UUID;

public class MeetingSelectionActivity extends AppCompatActivity {

    private ActivityMeetingSelectionBinding binding;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeetingSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userEmail = getIntent().getStringExtra("user_email");

        binding.buttonCreateMeeting.setOnClickListener(v -> {
            String meetingId = UUID.randomUUID().toString().substring(0, 8);
            startMeeting(meetingId);
        });

        binding.buttonJoinMeeting.setOnClickListener(v -> {
            String meetingId = binding.editMeetingId.getText().toString().trim();
            if (TextUtils.isEmpty(meetingId)) {
                binding.inputMeetingId.setError("Enter Meeting ID");
                return;
            }
            binding.inputMeetingId.setError(null);
            startMeeting(meetingId);
        });

        binding.textSignOut.setOnClickListener(v -> {
            Intent intent = new Intent(MeetingSelectionActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.buttonViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MeetingSelectionActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void startMeeting(String meetingId) {
        Intent intent = new Intent(MeetingSelectionActivity.this, ConferenceActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("meeting_id", meetingId);
        startActivity(intent);
    }
}
