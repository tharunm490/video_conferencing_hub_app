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
    private DatabaseHelper dbHelper;
    private String userEmail;
    private String userName = "Guest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeetingSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("user_email");
        
        android.util.Pair<String, String> userInfo = dbHelper.getUserInfo(userEmail);
        if (userInfo != null) {
            userName = userInfo.first;
            binding.textWelcome.setText(getString(R.string.welcome_user, userName));
        }

        binding.buttonCreateMeeting.setOnClickListener(v -> {
            String meetingId = UUID.randomUUID().toString().substring(0, 8);
            startMeeting(meetingId, true);
        });

        binding.buttonJoinMeeting.setOnClickListener(v -> {
            String meetingId = binding.editMeetingId.getText().toString().trim();
            if (TextUtils.isEmpty(meetingId)) {
                binding.inputMeetingId.setError(getString(R.string.enter_meeting_id));
                return;
            }
            binding.inputMeetingId.setError(null);
            startMeeting(meetingId, false);
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

        binding.buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MeetingSelectionActivity.this, ProfileActivity.class);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });

        binding.buttonCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MeetingSelectionActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
    }

    private void startMeeting(String meetingId, boolean isCaller) {
        Intent intent = new Intent(MeetingSelectionActivity.this, ConferenceActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_name", userName);
        intent.putExtra("meeting_id", meetingId);
        intent.putExtra("is_caller", isCaller);
        startActivity(intent);
    }
}
