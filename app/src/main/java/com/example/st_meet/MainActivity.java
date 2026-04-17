package com.example.st_meet;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.st_meet.databinding.ActivityMainBinding;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonSignIn.setOnClickListener(v -> validateAndContinue());
        binding.buttonGoogle.setOnClickListener(v -> {
            binding.editEmail.setText("demo@conferencehub.app");
            binding.editPassword.setText("password123");
            validateAndContinue();
        });
        binding.textForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Forgot Password flow can be added next.", Toast.LENGTH_SHORT).show());
        binding.textSignUp.setOnClickListener(v ->
                Toast.makeText(this, "Sign Up flow can be added next.", Toast.LENGTH_SHORT).show());
    }

    private void validateAndContinue() {
        String email = binding.editEmail.getText() == null ? "" : binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText() == null ? "" : binding.editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.inputEmail.setError("Enter email");
            return;
        }
        binding.inputEmail.setError(null);

        if (TextUtils.isEmpty(password)) {
            binding.inputPassword.setError("Enter password");
            return;
        }
        binding.inputPassword.setError(null);

        requestPermissionsAndOpenConference(email);
    }

    private void requestPermissionsAndOpenConference(String email) {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (cameraGranted && micGranted) {
            openConferenceWithDelay(email);
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void onPermissionResult(@NonNull Map<String, Boolean> result) {
        boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
        boolean micGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));

        if (cameraGranted && micGranted) {
            String email = binding.editEmail.getText() == null ? "Guest User" : binding.editEmail.getText().toString().trim();
            openConferenceWithDelay(email);
        } else {
            Toast.makeText(this, "Camera and microphone permissions are required.", Toast.LENGTH_LONG).show();
        }
    }

    private void openConferenceWithDelay(String email) {
        binding.buttonSignIn.setEnabled(false);
        binding.buttonSignIn.setText("Signing In...");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, MeetingSelectionActivity.class);
            intent.putExtra("user_email", email);
            startActivity(intent);
        }, 1600);
    }
}