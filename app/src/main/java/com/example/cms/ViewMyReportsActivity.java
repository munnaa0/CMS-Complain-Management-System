package com.example.cms;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewMyReportsActivity extends AppCompatActivity {

    private static final String TAG = "ViewMyReports";

    // UI Components
    private TextView institutionNameText;
    private LinearLayout reportsContainer;
    private Button backButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;
    private String institutionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_reports);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        institutionId = getIntent().getStringExtra("institutionId");
        institutionName = getIntent().getStringExtra("institutionName");

        // Initialize views
        initializeViews();

        // Display institution info
        if (institutionName != null) {
            institutionNameText.setText(institutionName);
        }

        // Load reports
        loadReports();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        reportsContainer = findViewById(R.id.reportsContainer);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadReports() {
        String userId = mAuth.getCurrentUser().getUid();

        // Clear existing views
        reportsContainer.removeAllViews();

        // Query reports for this user and institution
        // Note: orderBy with multiple whereEqualTo requires a composite index in Firestore
        // For now, we'll sort in code to avoid index requirement
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .whereEqualTo("institutionId", institutionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No reports found
                        TextView noReportsText = new TextView(this);
                        noReportsText.setText("You haven't submitted any reports yet.");
                        noReportsText.setTextSize(14);
                        noReportsText.setTextColor(Color.parseColor("#757575"));
                        noReportsText.setGravity(android.view.Gravity.CENTER);
                        noReportsText.setPadding(16, 16, 16, 16);
                        reportsContainer.addView(noReportsText);
                    } else {
                        // Sort reports by createdAt in descending order (newest first)
                        java.util.List<QueryDocumentSnapshot> sortedDocs = new java.util.ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            sortedDocs.add(doc);
                        }
                        sortedDocs.sort((doc1, doc2) -> {
                            Long time1 = doc1.getLong("createdAt");
                            Long time2 = doc2.getLong("createdAt");
                            if (time1 == null) time1 = 0L;
                            if (time2 == null) time2 = 0L;
                            return time2.compareTo(time1); // Descending order
                        });
                        
                        // Display reports
                        for (QueryDocumentSnapshot document : sortedDocs) {
                            String reportId = document.getId();
                            String title = document.getString("title");
                            String description = document.getString("description");
                            String status = document.getString("status");
                            String managerResponse = document.getString("managerResponse");
                            Long createdAt = document.getLong("createdAt");

                            addReportCard(reportId, title, description, status, managerResponse, createdAt);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reports", e);
                    Toast.makeText(this, "Error loading reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addReportCard(String reportId, String title, String description, 
                               String status, String managerResponse, Long createdAt) {
        // Create card layout
        androidx.constraintlayout.widget.ConstraintLayout reportCard = 
                new androidx.constraintlayout.widget.ConstraintLayout(this);
        reportCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        reportCard.setPadding(16, 16, 16, 16);
        reportCard.setBackgroundResource(R.drawable.report_item_background);
        
        // Set margin
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) reportCard.getLayoutParams();
        cardParams.setMargins(0, 0, 0, 16);
        reportCard.setLayoutParams(cardParams);

        // Report Title
        TextView titleText = new TextView(this);
        titleText.setId(View.generateViewId());
        titleText.setText(title != null ? title : "Untitled Report");
        titleText.setTextSize(16);
        titleText.setTextColor(Color.parseColor("#212121"));
        android.text.TextPaint paint = titleText.getPaint();
        paint.setFakeBoldText(true);
        
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams titleParams = 
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        titleText.setLayoutParams(titleParams);
        reportCard.addView(titleText);

        // Report Status
        TextView statusText = new TextView(this);
        statusText.setId(View.generateViewId());
        statusText.setText("Status: " + (status != null ? status.substring(0, 1).toUpperCase() + status.substring(1) : "Unknown"));
        statusText.setTextSize(14);
        statusText.setTextColor(getStatusColor(status));
        
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams statusParams = 
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topToBottom = titleText.getId();
        statusParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        statusParams.topMargin = 8;
        statusText.setLayoutParams(statusParams);
        reportCard.addView(statusText);

        // Report Date
        TextView dateText = new TextView(this);
        dateText.setId(View.generateViewId());
        String dateStr = "Unknown Date";
        if (createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateStr = sdf.format(new Date(createdAt));
        }
        dateText.setText("Date: " + dateStr);
        dateText.setTextSize(12);
        dateText.setTextColor(Color.parseColor("#757575"));
        
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams dateParams = 
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        dateParams.topToBottom = statusText.getId();
        dateParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        dateParams.topMargin = 4;
        dateText.setLayoutParams(dateParams);
        reportCard.addView(dateText);

        // Make card clickable to show details
        reportCard.setOnClickListener(v -> showReportDetails(title, description, status, managerResponse, createdAt));

        reportsContainer.addView(reportCard);
    }

    private int getStatusColor(String status) {
        if (status == null) return Color.parseColor("#757575");
        
        switch (status.toLowerCase()) {
            case "pending":
                return Color.parseColor("#FF9800"); // Orange
            case "investigating":
                return Color.parseColor("#2196F3"); // Blue
            case "verified":
                return Color.parseColor("#4CAF50"); // Green
            case "rejected":
                return Color.parseColor("#F44336"); // Red
            default:
                return Color.parseColor("#757575"); // Gray
        }
    }

    private void showReportDetails(String title, String description, String status, 
                                   String managerResponse, Long createdAt) {
        // Create dialog to show full report details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title != null ? title : "Report Details");

        // Build message
        StringBuilder message = new StringBuilder();
        
        // Date
        if (createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            message.append("Date: ").append(sdf.format(new Date(createdAt))).append("\n\n");
        }
        
        // Description
        message.append("Description:\n").append(description != null ? description : "No description").append("\n\n");
        
        // Status
        message.append("Status: ").append(status != null ? status.substring(0, 1).toUpperCase() + status.substring(1) : "Unknown").append("\n\n");
        
        // Manager Response
        if (managerResponse != null && !managerResponse.isEmpty()) {
            message.append("Manager Response:\n").append(managerResponse);
        } else {
            message.append("Manager Response: No response yet");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
