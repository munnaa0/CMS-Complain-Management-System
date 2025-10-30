package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageReportActivity extends AppCompatActivity {

    private static final String TAG = "ManageReport";

    // UI Components
    private TextView institutionNameText;
    private TextView reportTitleText;
    private TextView submittedByText;
    private TextView submittedOnText;
    private TextView descriptionText;
    private Spinner statusSpinner;
    private EditText responseEditText;
    private Button updateReportButton;
    private Button backButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String reportId;
    private String institutionName;
    private String currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_report);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        reportId = getIntent().getStringExtra("reportId");
        institutionName = getIntent().getStringExtra("institutionName");

        if (reportId == null) {
            Toast.makeText(this, "Error: Report ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Display institution name
        if (institutionName != null) {
            institutionNameText.setText(institutionName);
        }

        // Setup status spinner
        setupStatusSpinner();

        // Load report data
        loadReportData();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        reportTitleText = findViewById(R.id.reportTitleText);
        submittedByText = findViewById(R.id.submittedByText);
        submittedOnText = findViewById(R.id.submittedOnText);
        descriptionText = findViewById(R.id.descriptionText);
        statusSpinner = findViewById(R.id.statusSpinner);
        responseEditText = findViewById(R.id.responseEditText);
        updateReportButton = findViewById(R.id.updateReportButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupStatusSpinner() {
        // Create status options
        List<String> statusOptions = new ArrayList<>();
        statusOptions.add("Pending");
        statusOptions.add("Investigating");
        statusOptions.add("Verified");
        statusOptions.add("Rejected");

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        // Update Report button
        updateReportButton.setOnClickListener(v -> {
            Log.d(TAG, "Update Report button clicked");
            updateReport();
        });

        // Back button
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void loadReportData() {
        db.collection("reports").document(reportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get report data
                        String title = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");
                        String status = documentSnapshot.getString("status");
                        String managerResponse = documentSnapshot.getString("managerResponse");
                        String userId = documentSnapshot.getString("userId");
                        String userRole = documentSnapshot.getString("userRole");
                        Long createdAt = documentSnapshot.getLong("createdAt");

                        // Store current status
                        currentStatus = status;

                        // Display title
                        if (title != null) {
                            reportTitleText.setText(title);
                        }

                        // Display description
                        if (description != null) {
                            descriptionText.setText(description);
                        }

                        // Display submitted on date
                        if (createdAt != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                            String dateStr = sdf.format(new Date(createdAt));
                            submittedOnText.setText("Submitted on: " + dateStr);
                        }

                        // Set status spinner to current status
                        if (status != null) {
                            String capitalizedStatus = status.substring(0, 1).toUpperCase() + status.substring(1);
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) statusSpinner.getAdapter();
                            int position = adapter.getPosition(capitalizedStatus);
                            if (position >= 0) {
                                statusSpinner.setSelection(position);
                            }
                        }

                        // Display manager response
                        if (managerResponse != null && !managerResponse.isEmpty()) {
                            responseEditText.setText(managerResponse);
                        }

                        // Fetch user name
                        if (userId != null) {
                            loadUserInfo(userId, userRole);
                        } else {
                            submittedByText.setText("Submitted by: Unknown User" + 
                                    (userRole != null ? " - Role: " + userRole : ""));
                        }

                        Log.d(TAG, "Report data loaded successfully");
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading report data", e);
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadUserInfo(String userId, String userRole) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String displayText = "Submitted by: " + (fullName != null ? fullName : "Unknown User");
                        if (userRole != null && !userRole.isEmpty()) {
                            displayText += " - Role: " + userRole;
                        }
                        submittedByText.setText(displayText);
                    } else {
                        submittedByText.setText("Submitted by: Unknown User" + 
                                (userRole != null ? " - Role: " + userRole : ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info", e);
                    submittedByText.setText("Submitted by: Unknown User" + 
                            (userRole != null ? " - Role: " + userRole : ""));
                });
    }

    private void updateReport() {
        // Get selected status
        String selectedStatus = statusSpinner.getSelectedItem().toString().toLowerCase();
        
        // Get manager response
        String managerResponse = responseEditText.getText().toString().trim();

        // Disable button to prevent double submission
        updateReportButton.setEnabled(false);

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", selectedStatus);
        updates.put("managerResponse", managerResponse);
        updates.put("updatedAt", System.currentTimeMillis());

        // Update report in Firestore
        db.collection("reports").document(reportId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Report updated successfully");
                    Toast.makeText(this, "Report updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating report", e);
                    Toast.makeText(this, "Error updating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateReportButton.setEnabled(true);
                });
    }
}
