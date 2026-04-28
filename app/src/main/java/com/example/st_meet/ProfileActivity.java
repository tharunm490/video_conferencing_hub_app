package com.example.st_meet;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.st_meet.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private DatabaseHelper dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("user_email");

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadUserProfile();

        binding.buttonUpdateProfile.setOnClickListener(v -> updateProfile());
    }

    private void loadUserProfile() {
        android.util.Pair<String, String> userInfo = dbHelper.getUserInfo(userEmail);
        if (userInfo != null) {
            binding.editProfileName.setText(userInfo.first);
            binding.editProfileUsername.setText(userInfo.second);
            binding.textDisplayEmail.setText(userEmail);
            if (userInfo.second != null && !userInfo.second.isEmpty()) {
                binding.textProfileInitial.setText(userInfo.second.substring(0, 1).toUpperCase());
            }
        }
    }

    private void updateProfile() {
        String name = binding.editProfileName.getText().toString().trim();
        String username = binding.editProfileUsername.getText().toString().trim();
        String password = binding.editProfilePassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Name and Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.updateUser(userEmail, name, username, password);
        if (success) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            if (!username.isEmpty()) {
                binding.textProfileInitial.setText(username.substring(0, 1).toUpperCase());
            }
        } else {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }
}
