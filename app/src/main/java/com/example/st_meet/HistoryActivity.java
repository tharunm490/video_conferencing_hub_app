package com.example.st_meet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.st_meet.databinding.ActivityHistoryBinding;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        List<Meeting> meetings = dbHelper.getAllMeetings();

        if (meetings.isEmpty()) {
            binding.textNoHistory.setVisibility(View.VISIBLE);
            binding.recyclerHistory.setVisibility(View.GONE);
        } else {
            binding.textNoHistory.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.VISIBLE);

            MeetingAdapter adapter = new MeetingAdapter(meetings, meeting -> {
                Intent intent = new Intent(HistoryActivity.this, MeetingDetailActivity.class);
                intent.putExtra("meeting_id_db", meeting.getId());
                startActivity(intent);
            });

            binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerHistory.setAdapter(adapter);
        }
    }
}
