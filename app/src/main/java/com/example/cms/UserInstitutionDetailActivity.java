package com.example.cms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserInstitutionDetailActivity extends AppCompatActivity {

    private static final String TAG = "UserInstitutionDetail";

    // UI Components
    private TextView institutionNameText;
    private TextView userRoleText;
    private Button submitReportButton;
    private Button viewReportsButton;
    private Button backButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_institution_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Load user and institution data
        loadData();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        userRoleText = findViewById(R.id.userRoleText);
        submitReportButton = findViewById(R.id.submitReportButton);
        viewReportsButton = findViewById(R.id.viewReportsButton);
        backButton = findViewById(R.id.backButton);
    }

    private void loadData() {
        // Get data from intent
        institutionId = getIntent().getStringExtra("institutionId");
        String institutionName = getIntent().getStringExtra("institutionName");
        userRole = getIntent().getStringExtra("userRole");

        // Display data
        if (institutionName != null) {
            institutionNameText.setText(institutionName);
        } else if (institutionId != null) {
            // Fallback: load institution name from Firestore
            loadInstitutionName(institutionId);
        }

        if (userRole != null) {
            userRoleText.setText("Your Role: " + userRole);
        }
    }

    private void loadInstitutionName(String institutionId) {
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String institutionName = documentSnapshot.getString("institutionName");
                        if (institutionName != null) {
                            institutionNameText.setText(institutionName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institution name", e);
                });
    }

    private void setupListeners() {
        // Submit Report button
        submitReportButton.setOnClickListener(v -> {
            Log.d(TAG, "Submit Report button clicked");
            Intent intent = new Intent(UserInstitutionDetailActivity.this, SubmitReportActivity.class);
            intent.putExtra("institutionId", institutionId);
            intent.putExtra("institutionName", getIntent().getStringExtra("institutionName"));
            intent.putExtra("userRole", userRole);
            startActivity(intent);
        });

        // View My Reports button
        viewReportsButton.setOnClickListener(v -> {
            Log.d(TAG, "View My Reports button clicked");
            Intent intent = new Intent(UserInstitutionDetailActivity.this, ViewMyReportsActivity.class);
            intent.putExtra("institutionId", institutionId);
            intent.putExtra("institutionName", getIntent().getStringExtra("institutionName"));
            startActivity(intent);
        });

        // Back button
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
