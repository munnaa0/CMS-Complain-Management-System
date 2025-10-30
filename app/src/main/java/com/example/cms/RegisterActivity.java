package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI Components
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private RadioGroup userTypeRadioGroup;
    private RadioButton managerRadioButton;
    private RadioButton userRadioButton;
    private Button registerButton;
    private android.widget.TextView backToLoginTextView;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup);
        managerRadioButton = findViewById(R.id.managerRadioButton);
        userRadioButton = findViewById(R.id.userRadioButton);
        registerButton = findViewById(R.id.registerButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Register button click
        registerButton.setOnClickListener(v -> handleRegistration());

        // Back to login link click
        backToLoginTextView.setOnClickListener(v -> {
            Log.d(TAG, "Back to login clicked");
            finish(); // Go back to login screen
        });
    }

    private void handleRegistration() {
        Log.d(TAG, "Register button clicked");

        // Get input values
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String userType = managerRadioButton.isChecked() ? "manager" : "user";

        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Create user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration successful
                        Log.d(TAG, "createUserWithEmail:success");
                        String userId = mAuth.getCurrentUser().getUid();
                        
                        // Save user data to Firestore
                        saveUserToFirestore(userId, email, fullName, userType);
                    } else {
                        // Registration failed
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);
                        
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String fullName, String email, String password, 
                                   String confirmPassword) {
        // Validate full name
        if (isEmpty(fullName)) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return false;
        }

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
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate confirm password
        if (isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!arePasswordsMatching(password, confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
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

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    private boolean arePasswordsMatching(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    private void saveUserToFirestore(String userId, String email, String fullName, 
                                     String userType) {
        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("fullName", fullName);
        user.put("userType", userType);
        user.put("roleName", null);  // Will be set to first role when creating institution
        user.put("institutionId", null);
        user.put("userRole", null);
        user.put("institutions", new java.util.ArrayList<>());  // Initialize empty institutions array

        // Save to Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore successfully");
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    
                    Toast.makeText(RegisterActivity.this, 
                        "Registration successful! Please login.", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Sign out the user and go back to login
                    mAuth.signOut();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data to Firestore", e);
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    
                    Toast.makeText(RegisterActivity.this, 
                        "Error saving user data: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
    }
}
