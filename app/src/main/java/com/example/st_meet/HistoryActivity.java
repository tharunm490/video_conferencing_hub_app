package com.example.st_meet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.st_meet.databinding.ActivityHistoryBinding;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private DatabaseHelper dbHelper;
    private MeetingAdapter adapter;
    private List<Meeting> meetingList;

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
        meetingList = dbHelper.getAllMeetings();

        if (meetingList.isEmpty()) {
            showEmptyState();
        } else {
            binding.textNoHistory.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.VISIBLE);

            adapter = new MeetingAdapter(meetingList, new MeetingAdapter.OnMeetingListener() {
                @Override
                public void onItemClick(Meeting meeting) {
                    Intent intent = new Intent(HistoryActivity.this, MeetingDetailActivity.class);
                    intent.putExtra("meeting_id_db", meeting.getId());
                    startActivity(intent);
                }

                @Override
                public void onDeleteClick(Meeting meeting, int position) {
                    // Delete from Database
                    dbHelper.deleteMeeting(meeting.getId());
                    
                    // Remove from List and update UI
                    meetingList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, meetingList.size());
                    
                    Toast.makeText(HistoryActivity.this, R.string.meeting_deleted, Toast.LENGTH_SHORT).show();

                    // Check if list is now empty
                    if (meetingList.isEmpty()) {
                        showEmptyState();
                    }
                }
            });

            binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerHistory.setAdapter(adapter);
        }
    }

    private void showEmptyState() {
        binding.textNoHistory.setVisibility(View.VISIBLE);
        binding.recyclerHistory.setVisibility(View.GONE);
    }
}
