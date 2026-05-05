package com.example.st_meet;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.st_meet.databinding.ActivityMeetingDetailBinding;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingDetailActivity extends AppCompatActivity {

    private ActivityMeetingDetailBinding binding;
    private DatabaseHelper dbHelper;
    private List<Message> chatMessages = new ArrayList<>();

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

            // Load Chat History
            chatMessages = dbHelper.getMessages(meeting.getMeetingId());
            setupChatRecyclerView();
            setupContributionChart();
        }
    }

    private void setupChatRecyclerView() {
        if (chatMessages.isEmpty()) {
            binding.recyclerChatHistory.setVisibility(View.GONE);
            return;
        }
        
        // Using a simple adapter for the history
        binding.recyclerChatHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerChatHistory.setAdapter(new ChatHistoryAdapter(chatMessages));
    }

    private void setupContributionChart() {
        if (chatMessages.isEmpty()) {
            binding.contributionChart.setVisibility(View.GONE);
            return;
        }

        Map<String, Integer> userCount = new HashMap<>();
        for (Message msg : chatMessages) {
            String user = msg.getUserName();
            userCount.put(user, userCount.getOrDefault(user, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : userCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        binding.contributionChart.setData(data);
        binding.contributionChart.getDescription().setEnabled(false);
        binding.contributionChart.setCenterText("Contribution");
        binding.contributionChart.animateY(1000);
        binding.contributionChart.invalidate();
    }
}
