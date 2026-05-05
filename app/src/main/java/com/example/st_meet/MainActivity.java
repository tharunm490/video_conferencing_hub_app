package com.example.st_meet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.st_meet.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        String message = "Google sign in failed (Status Code: " + e.getStatusCode() + "): " + e.getMessage();
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        android.util.Log.e("GoogleSignIn", message);
                    }
                } else {
                    Toast.makeText(this, "Sign in cancelled or failed. Result Code: " + result.getResultCode(), Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.buttonSignIn.setOnClickListener(v -> validateAndSignIn());
        binding.buttonGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
        binding.textForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Forgot Password flow can be added next.", Toast.LENGTH_SHORT).show());
        binding.textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void validateAndSignIn() {
        String email = binding.editEmail.getText() == null ? "" : binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText() == null ? "" : binding.editPassword.getText().toString().trim();

        if (!isValidEmail(email)) {
            binding.inputEmail.setError("Enter a valid email");
            return;
        }
        binding.inputEmail.setError(null);

        if (TextUtils.isEmpty(password)) {
            binding.inputPassword.setError("Enter password");
            return;
        }
        binding.inputPassword.setError(null);

        if (dbHelper.checkUser(email, password)) {
            requestPermissionsAndOpenConference(email);
        } else {
            Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String email = mAuth.getCurrentUser().getEmail();
                        String displayName = mAuth.getCurrentUser().getDisplayName();
                        
                        // Sync with local database
                        if (!dbHelper.isUserExists(email)) {
                            dbHelper.insertUser(displayName, "google_user", email, "google_auth");
                        }

                        requestPermissionsAndOpenConference(email);
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
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
