package com.example.cms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class UserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "UserDashboard";

    // UI Components
    private TextView welcomeText;
    private LinearLayout institutionContainer;
    private Button joinInstitutionButton;
    private Button logoutButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Flag to track if this is first load
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();

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
        // Reload user data when returning to this activity (but skip on first load)
        if (!isFirstLoad) {
            loadUserData();
        }
        isFirstLoad = false;
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        institutionContainer = findViewById(R.id.institutionContainer);
        joinInstitutionButton = findViewById(R.id.joinInstitutionButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();

        // Clear existing institution cards
        institutionContainer.removeAllViews();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        
                        if (fullName != null) {
                            welcomeText.setText("Welcome, " + fullName + "!");
                        }

                        // Get institutions array
                        List<Map<String, Object>> institutions = null;
                        if (documentSnapshot.contains("institutions")) {
                            institutions = (List<Map<String, Object>>) documentSnapshot.get("institutions");
                        }

                        // Check if user has joined any institutions
                        if (institutions != null && !institutions.isEmpty()) {
                            // User has joined institutions - always show join button for multiple institutions
                            joinInstitutionButton.setVisibility(View.VISIBLE);
                            institutionContainer.setVisibility(View.VISIBLE);

                            // Load and display all institutions as cards
                            for (Map<String, Object> institution : institutions) {
                                String institutionId = (String) institution.get("institutionId");
                                String role = (String) institution.get("role");
                                Boolean isManager = (Boolean) institution.get("isManager");
                                
                                if (institutionId != null) {
                                    loadInstitutionCard(institutionId, role, isManager != null && isManager);
                                }
                            }
                        } else {
                            // User has not joined any institution
                            joinInstitutionButton.setVisibility(View.VISIBLE);
                            institutionContainer.setVisibility(View.VISIBLE);
                            
                            // Show message
                            TextView noInstitutionText = new TextView(this);
                            noInstitutionText.setText("You haven't joined any institution yet.\nClick 'Join Institution' to get started!");
                            noInstitutionText.setTextSize(14);
                            noInstitutionText.setTextColor(Color.parseColor("#757575"));
                            noInstitutionText.setGravity(android.view.Gravity.CENTER);
                            noInstitutionText.setPadding(16, 16, 16, 16);
                            institutionContainer.addView(noInstitutionText);
                        }
                        
                        Log.d(TAG, "User data loaded: " + fullName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadInstitutionCard(String institutionId, String userRole, boolean isManager) {
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String institutionName = documentSnapshot.getString("institutionName");
                        String managerRoleName = documentSnapshot.getString("managerRoleName");

                        // Create a card for the institution
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
                        nameText.setId(View.generateViewId());
                        nameText.setText(institutionName != null ? institutionName : "Institution");
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

                        // User Role with manager badge
                        TextView roleText = new TextView(this);
                        roleText.setId(View.generateViewId());
                        String roleDisplay = "Your Role: " + (userRole != null ? userRole : "Member");
                        if (isManager) {
                            roleDisplay += " (Manager)";
                        }
                        roleText.setText(roleDisplay);
                        roleText.setTextSize(14);
                        roleText.setTextColor(isManager ? Color.parseColor("#2196F3") : Color.parseColor("#757575"));
                        
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams roleParams = 
                                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        roleParams.topToBottom = nameText.getId();
                        roleParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                        roleParams.topMargin = 8;
                        roleText.setLayoutParams(roleParams);
                        institutionCard.addView(roleText);

                        // Click instruction
                        TextView clickText = new TextView(this);
                        clickText.setId(View.generateViewId());
                        clickText.setText(isManager ? "Tap to manage" : "Tap to view options");
                        clickText.setTextSize(12);
                        clickText.setTextColor(Color.parseColor("#2196F3"));
                        
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams clickParams = 
                                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        clickParams.topToBottom = roleText.getId();
                        clickParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                        clickParams.topMargin = 8;
                        clickText.setLayoutParams(clickParams);
                        institutionCard.addView(clickText);

                        // Make the card clickable - go to manager view if manager, user view otherwise
                        institutionCard.setOnClickListener(v -> {
                            if (isManager) {
                                Intent intent = new Intent(UserDashboardActivity.this, InstitutionDetailActivity.class);
                                intent.putExtra("institutionId", institutionId);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(UserDashboardActivity.this, UserInstitutionDetailActivity.class);
                                intent.putExtra("institutionId", institutionId);
                                intent.putExtra("institutionName", institutionName);
                                intent.putExtra("userRole", userRole);
                                startActivity(intent);
                            }
                        });

                        institutionContainer.addView(institutionCard);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institution", e);
                });
    }

    private void setupListeners() {
        // Join Institution button click
        joinInstitutionButton.setOnClickListener(v -> {
            Log.d(TAG, "Join Institution button clicked");
            Intent intent = new Intent(UserDashboardActivity.this, JoinInstitutionActivity.class);
            startActivity(intent);
        });

        // Logout button click
        logoutButton.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate back to login
            Intent intent = new Intent(UserDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
