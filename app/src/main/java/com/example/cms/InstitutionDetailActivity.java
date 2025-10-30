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

import java.util.List;

public class InstitutionDetailActivity extends AppCompatActivity {

    private static final String TAG = "InstitutionDetail";

    // UI Components
    private TextView institutionNameText;
    private TextView managerInfoText;
    private TextView rolesListText;
    private TextView reportsStatsText;
    private Button addRolesButton;
    private Button viewReportsButton;
    private Button backButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;
    
    // Flag to track if this is first load
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get institution ID from intent
        institutionId = getIntent().getStringExtra("institutionId");

        if (institutionId == null) {
            Toast.makeText(this, "Error: Institution ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Load institution data
        loadInstitutionData();

        // Load reports statistics
        loadReportsStatistics();

        // Set up listeners
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload institution data when returning to this activity (but skip on first load)
        if (!isFirstLoad) {
            loadInstitutionData();
            loadReportsStatistics();
        }
        isFirstLoad = false;
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        managerInfoText = findViewById(R.id.managerInfoText);
        rolesListText = findViewById(R.id.rolesListText);
        reportsStatsText = findViewById(R.id.reportsStatsText);
        addRolesButton = findViewById(R.id.addRolesButton);
        viewReportsButton = findViewById(R.id.viewReportsButton);
        backButton = findViewById(R.id.backButton);
    }

    private void loadInstitutionData() {
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String institutionName = documentSnapshot.getString("institutionName");
                        String managerId = documentSnapshot.getString("managerId");
                        String managerRoleName = documentSnapshot.getString("managerRoleName");
                        List<String> roles = (List<String>) documentSnapshot.get("roles");

                        // Set institution name
                        if (institutionName != null) {
                            institutionNameText.setText(institutionName);
                        }

                        // Load manager name
                        if (managerId != null) {
                            loadManagerName(managerId, managerRoleName);
                        }

                        // Set roles list
                        if (roles != null && !roles.isEmpty()) {
                            String rolesText = String.join(", ", roles);
                            rolesListText.setText(rolesText);
                        } else {
                            rolesListText.setText("No roles defined yet.");
                        }

                        Log.d(TAG, "Institution data loaded: " + institutionName);
                    } else {
                        Toast.makeText(this, "Institution not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institution data", e);
                    Toast.makeText(this, "Error loading institution data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadManagerName(String managerId, String managerRoleName) {
        db.collection("users").document(managerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String managerName = documentSnapshot.getString("fullName");
                        if (managerName != null && managerRoleName != null) {
                            managerInfoText.setText("Manager: " + managerName + " (" + managerRoleName + ")");
                        } else if (managerName != null) {
                            managerInfoText.setText("Manager: " + managerName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading manager name", e);
                });
    }

    private void setupListeners() {
        // Add More Roles button
        addRolesButton.setOnClickListener(v -> {
            Log.d(TAG, "Add More Roles button clicked");
            Intent intent = new Intent(InstitutionDetailActivity.this, AddRolesActivity.class);
            intent.putExtra("institutionId", institutionId);
            startActivity(intent);
        });

        // View All Reports button
        viewReportsButton.setOnClickListener(v -> {
            Log.d(TAG, "View All Reports button clicked");
            Intent intent = new Intent(InstitutionDetailActivity.this, ViewAllReportsActivity.class);
            intent.putExtra("institutionId", institutionId);
            // Get institution name from the TextView
            String instName = institutionNameText.getText().toString();
            intent.putExtra("institutionName", instName);
            startActivity(intent);
        });

        // Back button
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void loadReportsStatistics() {
        db.collection("reports")
                .whereEqualTo("institutionId", institutionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalReports = queryDocumentSnapshots.size();
                    int pendingCount = 0;
                    int investigatingCount = 0;
                    int verifiedCount = 0;
                    int rejectedCount = 0;

                    // Count reports by status
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if (status != null) {
                            switch (status.toLowerCase()) {
                                case "pending":
                                    pendingCount++;
                                    break;
                                case "investigating":
                                    investigatingCount++;
                                    break;
                                case "verified":
                                    verifiedCount++;
                                    break;
                                case "rejected":
                                    rejectedCount++;
                                    break;
                            }
                        }
                    }

                    // Update the UI
                    String statsText = "Total: " + totalReports + " | " +
                            "Pending: " + pendingCount + " | " +
                            "Investigating: " + investigatingCount + " | " +
                            "Verified: " + verifiedCount + " | " +
                            "Rejected: " + rejectedCount;
                    reportsStatsText.setText(statsText);

                    Log.d(TAG, "Reports statistics loaded: " + statsText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reports statistics", e);
                    reportsStatsText.setText("Error loading reports statistics");
                });
    }
}
