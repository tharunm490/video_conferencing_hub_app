package com.example.st_meet;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.st_meet.databinding.ActivityParticipantsBinding;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsActivity extends AppCompatActivity {

    private ActivityParticipantsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityParticipantsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupParticipants();
    }

    private void setupParticipants() {
        List<String> participants = new ArrayList<>();
        participants.add(getString(R.string.you_host));
        participants.add(getString(R.string.alex));
        participants.add(getString(R.string.sara));
        participants.add(getString(R.string.john));
        participants.add(getString(R.string.emily));
        participants.add(getString(R.string.michael));

        ParticipantAdapter adapter = new ParticipantAdapter(participants);
        binding.recyclerParticipantsList.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerParticipantsList.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
