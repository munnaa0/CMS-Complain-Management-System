package com.example.cms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewAllReportsActivity extends AppCompatActivity {

    private static final String TAG = "ViewAllReports";

    // UI Components
    private TextView institutionNameText;
    private Spinner statusFilterSpinner;
    private LinearLayout reportsContainer;
    private Button backButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;
    private String institutionName;
    private String selectedStatusFilter = "All";
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_reports);

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
        } else if (institutionId != null) {
            loadInstitutionName();
        }

        // Setup status filter spinner
        setupStatusFilter();

        // Load reports
        loadReports();

        // Set up listeners
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload reports when returning to this activity (but not on first load)
        if (!isInitialLoad) {
            loadReports();
        }
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);
        reportsContainer = findViewById(R.id.reportsContainer);
        backButton = findViewById(R.id.backButton);
    }

    private void loadInstitutionName() {
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("institutionName");
                        if (name != null) {
                            institutionName = name;
                            institutionNameText.setText(name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institution name", e);
                });
    }

    private void setupStatusFilter() {
        // Create status filter options
        List<String> statusOptions = new ArrayList<>();
        statusOptions.add("All");
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
        statusFilterSpinner.setAdapter(adapter);

        // Set listener
        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatusFilter = statusOptions.get(position);
                Log.d(TAG, "Filter changed to: " + selectedStatusFilter);
                // Skip loading on initial setup (will be loaded in onCreate)
                if (!isInitialLoad) {
                    loadReports();
                }
                isInitialLoad = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadReports() {
        // Clear existing views
        reportsContainer.removeAllViews();

        // Query reports for this institution
        db.collection("reports")
                .whereEqualTo("institutionId", institutionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No reports found
                        TextView noReportsText = new TextView(this);
                        noReportsText.setText("No reports submitted yet.");
                        noReportsText.setTextSize(14);
                        noReportsText.setTextColor(Color.parseColor("#757575"));
                        noReportsText.setGravity(android.view.Gravity.CENTER);
                        noReportsText.setPadding(16, 16, 16, 16);
                        reportsContainer.addView(noReportsText);
                    } else {
                        // Sort reports by createdAt in descending order (newest first)
                        List<QueryDocumentSnapshot> sortedDocs = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // Apply status filter
                            String status = doc.getString("status");
                            if (selectedStatusFilter.equals("All") || 
                                (status != null && status.equalsIgnoreCase(selectedStatusFilter))) {
                                sortedDocs.add(doc);
                            }
                        }

                        if (sortedDocs.isEmpty()) {
                            // No reports match the filter
                            TextView noReportsText = new TextView(this);
                            noReportsText.setText("No reports with status: " + selectedStatusFilter);
                            noReportsText.setTextSize(14);
                            noReportsText.setTextColor(Color.parseColor("#757575"));
                            noReportsText.setGravity(android.view.Gravity.CENTER);
                            noReportsText.setPadding(16, 16, 16, 16);
                            reportsContainer.addView(noReportsText);
                            return;
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
                            String userId = document.getString("userId");
                            String userRole = document.getString("userRole");
                            String status = document.getString("status");
                            Long createdAt = document.getLong("createdAt");

                            addReportCard(reportId, title, userId, userRole, status, createdAt);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reports", e);
                    Toast.makeText(this, "Error loading reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addReportCard(String reportId, String title, String userId, 
                               String userRole, String status, Long createdAt) {
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

        // Submitted By Text (will be updated after fetching user email)
        TextView submittedByText = new TextView(this);
        submittedByText.setId(View.generateViewId());
        submittedByText.setText("Loading user info...");
        submittedByText.setTextSize(14);
        submittedByText.setTextColor(Color.parseColor("#757575"));

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams submittedByParams =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        submittedByParams.topToBottom = titleText.getId();
        submittedByParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        submittedByParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        submittedByParams.topMargin = 4;
        submittedByText.setLayoutParams(submittedByParams);
        reportCard.addView(submittedByText);

        // Fetch user email
        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String email = documentSnapshot.getString("email");
                            String displayText = "By: " + (email != null ? email : "Unknown User");
                            if (userRole != null && !userRole.isEmpty()) {
                                displayText += " - Role: " + userRole;
                            }
                            submittedByText.setText(displayText);
                        } else {
                            submittedByText.setText("By: Unknown User" + 
                                    (userRole != null ? " - Role: " + userRole : ""));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user email", e);
                        submittedByText.setText("By: Unknown User" + 
                                (userRole != null ? " - Role: " + userRole : ""));
                    });
        } else {
            submittedByText.setText("By: Unknown User" + 
                    (userRole != null ? " - Role: " + userRole : ""));
        }

        // Report Status
        TextView statusText = new TextView(this);
        statusText.setId(View.generateViewId());
        String statusDisplay = status != null ? 
                status.substring(0, 1).toUpperCase() + status.substring(1) : "Unknown";
        statusText.setText("Status: " + statusDisplay);
        statusText.setTextSize(14);
        statusText.setTextColor(getStatusColor(status));

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams statusParams =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topToBottom = submittedByText.getId();
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
        dateText.setText(dateStr);
        dateText.setTextSize(12);
        dateText.setTextColor(Color.parseColor("#757575"));

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams dateParams =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
        dateParams.topToBottom = submittedByText.getId();
        dateParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        dateParams.topMargin = 8;
        dateText.setLayoutParams(dateParams);
        reportCard.addView(dateText);

        // Set click listener to open report detail
        reportCard.setOnClickListener(v -> {
            Intent intent = new Intent(ViewAllReportsActivity.this, ManageReportActivity.class);
            intent.putExtra("reportId", reportId);
            intent.putExtra("institutionName", institutionName);
            startActivity(intent);
        });

        // Add card to container
        reportsContainer.addView(reportCard);
    }

    private int getStatusColor(String status) {
        if (status == null) {
            return Color.parseColor("#757575"); // Gray for unknown
        }
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
}
