package com.example.st_meet;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.st_meet.databinding.ActivityConferenceBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class ConferenceActivity extends AppCompatActivity {

    private ActivityConferenceBinding binding;
    private boolean isMicOn = true;
    private boolean isCameraOn = true;
    private String userEmail = "Guest";
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private ProcessCameraProvider cameraProvider;
    private DatabaseHelper dbHelper;
    private String meetingId = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        if (getIntent() != null) {
            userEmail = getIntent().getStringExtra("user_email") != null ? getIntent().getStringExtra("user_email") : "Guest";
            meetingId = getIntent().getStringExtra("meeting_id") != null ? getIntent().getStringExtra("meeting_id") : "Daily Sync";
            binding.textMeetingTitle.setText("Meeting: " + meetingId);
        }

        setupParticipants();
        setupClicks();
        updateMicUi();
        updateCameraUi();

        if (checkPermissions()) {
            startCamera();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions are required for video calling", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            Toast.makeText(this, "Binding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupParticipants() {
        List<String> participants = new ArrayList<>();
        participants.add("Alex");
        participants.add("Sara");
        participants.add("John");

        ParticipantAdapter adapter = new ParticipantAdapter(participants);
        binding.recyclerParticipants.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerParticipants.setAdapter(adapter);
    }

    private void setupClicks() {
        binding.buttonMic.setOnClickListener(v -> {
            isMicOn = !isMicOn;
            updateMicUi();
            Toast.makeText(this, isMicOn ? "Microphone On" : "Microphone Off", Toast.LENGTH_SHORT).show();
        });

        binding.buttonCamera.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            updateCameraUi();
            if (isCameraOn) {
                startCamera();
            } else {
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
            }
        });

        binding.buttonSwitchCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT) ?
                    CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
            if (isCameraOn) {
                bindCameraPreview();
            }
        });

        binding.buttonEndCall.setOnClickListener(v -> {
            saveMeetingToHistory();
            finish();
        });

        binding.buttonChat.setOnClickListener(v ->
                Toast.makeText(this, "Chat feature next.", Toast.LENGTH_SHORT).show());

        binding.buttonParticipants.setOnClickListener(v -> {
            Intent intent = new Intent(ConferenceActivity.this, ParticipantsActivity.class);
            startActivity(intent);
        });

        binding.buttonShare.setOnClickListener(v ->
                Toast.makeText(this, "Screen share feature next.", Toast.LENGTH_SHORT).show());

        binding.buttonAiSummary.setOnClickListener(v -> showAiSummarySheet());

        binding.btnReactThumb.setOnClickListener(v -> showReaction("👍"));
        binding.btnReactHeart.setOnClickListener(v -> showReaction("❤️"));
        binding.btnReactParty.setOnClickListener(v -> showReaction("🎉"));
        binding.btnReactClap.setOnClickListener(v -> showReaction("👏"));
    }

    private void showAiSummarySheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_ai_summary_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        View loadingLayout = sheetView.findViewById(R.id.layoutLoading);
        View contentLayout = sheetView.findViewById(R.id.layoutSummaryContent);
        View btnRegenerate = sheetView.findViewById(R.id.btnRegenerate);

        btnRegenerate.setOnClickListener(v -> {
            contentLayout.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadingLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }, 2000);
        });

        bottomSheetDialog.show();
    }

    private void showReaction(String emoji) {
        TextView reactionText = new TextView(this);
        reactionText.setText(emoji);
        reactionText.setTextSize(40);
        reactionText.setX(new Random().nextInt(binding.reactionContainer.getWidth() - 100));
        reactionText.setY(binding.reactionContainer.getHeight());
        reactionText.setAlpha(1f);

        binding.reactionContainer.addView(reactionText);

        reactionText.animate()
                .translationYBy(-600 - new Random().nextInt(400))
                .alpha(0f)
                .setDuration(3000)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.reactionContainer.removeView(reactionText);
                    }
                })
                .start();
    }

    private void updateMicUi() {
        if (isMicOn) {
            binding.buttonMic.setImageResource(R.drawable.ic_mic);
        } else {
            binding.buttonMic.setImageResource(R.drawable.ic_mic_off);
        }
    }

    private void updateCameraUi() {
        if (isCameraOn) {
            binding.buttonCamera.setImageResource(R.drawable.ic_videocam);
            binding.previewView.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.buttonCamera.setImageResource(R.drawable.ic_videocam_off);
            binding.previewView.setVisibility(android.view.View.GONE);
        }
    }

    private void saveMeetingToHistory() {
        String timestamp = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        String dummySummary = "This meeting was held with participants to discuss project updates. " +
                "Key points included CameraX integration and SQLite history implementation. " +
                "The team successfully demonstrated floating reactions and AI summary mockups.";

        dbHelper.insertMeeting(meetingId, timestamp, dummySummary);
    }
}
