package com.example.st_meet;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.st_meet.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.buttonSignUp.setOnClickListener(v -> validateAndSignUp());

        binding.textSignIn.setOnClickListener(v -> finish());
    }

    private void validateAndSignUp() {
        String name = binding.editName.getText().toString().trim();
        String username = binding.editUsername.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.inputName.setError("Enter name");
            return;
        }
        binding.inputName.setError(null);

        if (TextUtils.isEmpty(username)) {
            binding.inputUsername.setError("Enter username");
            return;
        }
        binding.inputUsername.setError(null);

        if (!isValidEmail(email)) {
            binding.inputEmail.setError("Enter a valid email");
            return;
        }
        binding.inputEmail.setError(null);

        if (password.length() < 6) {
            binding.inputPassword.setError("Password must be at least 6 characters");
            return;
        }
        binding.inputPassword.setError(null);

        if (dbHelper.isUserExists(email)) {
            binding.inputEmail.setError("Email already exists");
            return;
        }

        if (dbHelper.insertUser(name, username, email, password)) {
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
