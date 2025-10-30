package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SubmitReportActivity extends AppCompatActivity {

    private static final String TAG = "SubmitReport";

    // UI Components
    private TextView institutionNameText;
    private EditText reportTitleEditText;
    private EditText reportDescriptionEditText;
    private Button submitButton;
    private Button cancelButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;
    private String institutionName;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_report);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        institutionId = getIntent().getStringExtra("institutionId");
        institutionName = getIntent().getStringExtra("institutionName");
        userRole = getIntent().getStringExtra("userRole");

        // Initialize views
        initializeViews();

        // Display institution info
        if (institutionName != null) {
            institutionNameText.setText(institutionName);
        }

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        reportTitleEditText = findViewById(R.id.reportTitleEditText);
        reportDescriptionEditText = findViewById(R.id.reportDescriptionEditText);
        submitButton = findViewById(R.id.submitButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupListeners() {
        // Submit button
        submitButton.setOnClickListener(v -> {
            if (validateInputs()) {
                submitReport();
            }
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> finish());
    }

    private boolean validateInputs() {
        String title = reportTitleEditText.getText().toString().trim();
        String description = reportDescriptionEditText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter report title", Toast.LENGTH_SHORT).show();
            reportTitleEditText.requestFocus();
            return false;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter report description", Toast.LENGTH_SHORT).show();
            reportDescriptionEditText.requestFocus();
            return false;
        }

        if (institutionId == null || institutionId.isEmpty()) {
            Toast.makeText(this, "Error: Institution ID not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void submitReport() {
        String userId = mAuth.getCurrentUser().getUid();
        String title = reportTitleEditText.getText().toString().trim();
        String description = reportDescriptionEditText.getText().toString().trim();
        long timestamp = System.currentTimeMillis();

        // Disable button to prevent double submission
        submitButton.setEnabled(false);

        // Create report data using HashMap
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("userId", userId);
        reportData.put("institutionId", institutionId);
        reportData.put("institutionName", institutionName);
        reportData.put("userRole", userRole);
        reportData.put("title", title);
        reportData.put("description", description);
        reportData.put("status", "pending");
        reportData.put("managerResponse", "");
        reportData.put("createdAt", timestamp);
        reportData.put("updatedAt", timestamp);

        // Save to Firestore
        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Report submitted successfully: " + documentReference.getId());
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to previous screen
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting report", e);
                    Toast.makeText(this, "Error submitting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true); // Re-enable button
                });
    }
}
