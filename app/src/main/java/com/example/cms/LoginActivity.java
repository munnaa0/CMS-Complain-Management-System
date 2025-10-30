package com.example.cms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Components
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views first (needed for progress bar)
        initializeViews();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in, redirecting to dashboard");
            String userId = mAuth.getCurrentUser().getUid();
            fetchUserDataAndRoute(userId);
            return;
        }

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> handleLogin());

        registerTextView.setOnClickListener(v -> {
            Log.d(TAG, "Register link clicked");
            // Navigate to RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        Log.d(TAG, "Login button clicked");

        // Get input values
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Show progress bar
        progressBar.setVisibility(android.view.View.VISIBLE);
        loginButton.setEnabled(false);

        // Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful
                        Log.d(TAG, "signInWithEmail:success");
                        String userId = mAuth.getCurrentUser().getUid();
                        
                        // Fetch user data and route to appropriate dashboard
                        fetchUserDataAndRoute(userId);
                    } else {
                        // Login failed
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        progressBar.setVisibility(android.view.View.GONE);
                        loginButton.setEnabled(true);
                        
                        String errorMessage = "Login failed. Please check your credentials.";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        // Validate email
        if (isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate password
        if (isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void fetchUserDataAndRoute(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    loginButton.setEnabled(true);

                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        String fullName = documentSnapshot.getString("fullName");
                        
                        Log.d(TAG, "User type: " + userType + ", Name: " + fullName);

                        if (userType != null) {
                            Intent intent;
                            if (userType.equals("manager")) {
                                // Navigate to Manager Dashboard
                                intent = new Intent(LoginActivity.this, ManagerDashboardActivity.class);
                            } else {
                                // Navigate to User Dashboard
                                intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                            }
                            
                            // Clear the back stack so user can't go back to login
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // User data not found in Firestore - sign out and show error
                        Log.e(TAG, "User authenticated but no Firestore document found");
                        mAuth.signOut();
                        Toast.makeText(this, "User data not found. Please register again.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    progressBar.setVisibility(android.view.View.GONE);
                    loginButton.setEnabled(true);
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
}
