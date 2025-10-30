package com.example.cms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ManagerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ManagerDashboard";

    // UI Components
    private TextView welcomeText;
    private Button createInstitutionButton;
    private Button logoutButton;
    private LinearLayout institutionsContainer;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Flag to track if this is first load
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();
        
        // Load institutions
        loadInstitutions();

        // Set up listeners
        setupListeners();

        // Disable back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Disable back button on dashboard
                // User must logout to go back
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload institutions when returning to this activity (but skip on first load)
        if (!isFirstLoad) {
            loadInstitutions();
        }
        isFirstLoad = false;
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        createInstitutionButton = findViewById(R.id.createInstitutionButton);
        logoutButton = findViewById(R.id.logoutButton);
        institutionsContainer = findViewById(R.id.institutionsContainer);
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String roleName = documentSnapshot.getString("roleName");
                        
                        if (fullName != null) {
                            welcomeText.setText("Welcome, " + fullName + "!");
                        }
                        
                        Log.d(TAG, "User data loaded: " + fullName + " (" + roleName + ")");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        // Create Institution button click
        createInstitutionButton.setOnClickListener(v -> {
            Log.d(TAG, "Create Institution button clicked");
            // Navigate to CreateInstitutionActivity
            Intent intent = new Intent(ManagerDashboardActivity.this, CreateInstitutionActivity.class);
            startActivity(intent);
        });

        // Logout button click
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate back to login
            Intent intent = new Intent(ManagerDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadInstitutions() {
        String userId = mAuth.getCurrentUser().getUid();

        // Clear existing institutions
        institutionsContainer.removeAllViews();

        // Query for institutions where user is a manager
        // This will get institutions from both old (managerId) and new (managerIds array) format
        db.collection("institutions")
                .whereArrayContains("managerIds", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Try old format for backwards compatibility
                        db.collection("institutions")
                                .whereEqualTo("managerId", userId)
                                .get()
                                .addOnSuccessListener(oldFormatSnapshots -> {
                                    if (oldFormatSnapshots.isEmpty()) {
                                        // No institutions found
                                        TextView noInstitutionsText = new TextView(this);
                                        noInstitutionsText.setText("No institutions yet. Create one to get started!");
                                        noInstitutionsText.setTextSize(14);
                                        noInstitutionsText.setTextColor(Color.parseColor("#757575"));
                                        noInstitutionsText.setGravity(Gravity.CENTER);
                                        noInstitutionsText.setPadding(16, 16, 16, 16);
                                        institutionsContainer.addView(noInstitutionsText);
                                    } else {
                                        displayInstitutions(oldFormatSnapshots);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading institutions", e);
                                    Toast.makeText(this, "Error loading institutions", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        displayInstitutions(queryDocumentSnapshots);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institutions", e);
                    Toast.makeText(this, "Error loading institutions", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayInstitutions(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        if (queryDocumentSnapshots.isEmpty()) {
            return;
        }
        
        // Display institutions
        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            String institutionId = document.getId();
            String institutionName = document.getString("institutionName");
            String managerRoleName = document.getString("managerRoleName");
            java.util.List<String> roles = (java.util.List<String>) document.get("roles");

            // Create a card for each institution
            androidx.constraintlayout.widget.ConstraintLayout institutionCard = 
                    new androidx.constraintlayout.widget.ConstraintLayout(this);
            institutionCard.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            institutionCard.setPadding(16, 16, 16, 16);
            
            // Set background
            institutionCard.setBackgroundResource(R.drawable.report_item_background);
            
            // Set margin
            LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) institutionCard.getLayoutParams();
            cardParams.setMargins(0, 0, 0, 16);
            institutionCard.setLayoutParams(cardParams);

            // Institution Name
            TextView nameText = new TextView(this);
            nameText.setId(android.view.View.generateViewId());
            nameText.setText(institutionName);
            nameText.setTextSize(18);
            nameText.setTextColor(Color.parseColor("#212121"));
            android.text.TextPaint paint = nameText.getPaint();
            paint.setFakeBoldText(true);
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams nameParams = 
                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
            nameParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            nameParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            nameParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            nameText.setLayoutParams(nameParams);
            institutionCard.addView(nameText);

            // Manager Role
            TextView managerText = new TextView(this);
            managerText.setId(android.view.View.generateViewId());
            managerText.setText("Your Role: " + (managerRoleName != null ? managerRoleName : "Manager"));
            managerText.setTextSize(14);
            managerText.setTextColor(Color.parseColor("#757575"));
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams managerParams = 
                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
            managerParams.topToBottom = nameText.getId();
            managerParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            managerParams.topMargin = 8;
            managerText.setLayoutParams(managerParams);
            institutionCard.addView(managerText);

            // Available Roles Count
            TextView rolesText = new TextView(this);
            rolesText.setId(android.view.View.generateViewId());
            int rolesCount = roles != null ? roles.size() : 0;
            rolesText.setText("Available Roles: " + rolesCount);
            rolesText.setTextSize(14);
            rolesText.setTextColor(Color.parseColor("#757575"));
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams rolesParams = 
                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
            rolesParams.topToBottom = managerText.getId();
            rolesParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            rolesParams.topMargin = 4;
            rolesText.setLayoutParams(rolesParams);
            institutionCard.addView(rolesText);

            // Institution ID (small text)
            TextView idText = new TextView(this);
            idText.setId(android.view.View.generateViewId());
            idText.setText("ID: " + institutionId);
            idText.setTextSize(12);
            idText.setTextColor(Color.parseColor("#9E9E9E"));
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams idParams = 
                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            0,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
            idParams.topToBottom = rolesText.getId();
            idParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            idParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            idParams.topMargin = 8;
            idText.setLayoutParams(idParams);
            institutionCard.addView(idText);

            // Make the card clickable
            institutionCard.setOnClickListener(v -> {
                Intent intent = new Intent(ManagerDashboardActivity.this, InstitutionDetailActivity.class);
                intent.putExtra("institutionId", institutionId);
                startActivity(intent);
            });

            institutionsContainer.addView(institutionCard);
        }

        Log.d(TAG, "Loaded " + queryDocumentSnapshots.size() + " institutions");
    }
}
