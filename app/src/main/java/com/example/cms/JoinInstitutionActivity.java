package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinInstitutionActivity extends AppCompatActivity {

    private static final String TAG = "JoinInstitution";

    // UI Components
    private EditText institutionNameEditText;
    private Button searchButton;
    private ConstraintLayout institutionDetailsContainer;
    private TextView institutionNameText;
    private RadioGroup rolesRadioGroup;
    private Button joinButton;
    private Button cancelButton;
    private LinearLayout institutionsListContainer;
    private ProgressBar progressBar;
    private ScrollView mainScrollView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String currentInstitutionId;
    private String currentInstitutionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_institution);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Check if user already joined an institution
        checkUserInstitution();

        // Load all available institutions
        loadAllInstitutions();
    }

    private void initializeViews() {
        // Find ScrollView by traversing the view hierarchy
        View decorView = getWindow().getDecorView();
        View rootView = decorView.findViewById(android.R.id.content);
        if (rootView instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) rootView;
            if (viewGroup.getChildCount() > 0) {
                View child = viewGroup.getChildAt(0);
                if (child instanceof ScrollView) {
                    mainScrollView = (ScrollView) child;
                }
            }
        }
        
        institutionNameEditText = findViewById(R.id.institutionNameEditText);
        searchButton = findViewById(R.id.searchButton);
        institutionDetailsContainer = findViewById(R.id.institutionDetailsContainer);
        institutionNameText = findViewById(R.id.institutionNameText);
        rolesRadioGroup = findViewById(R.id.rolesRadioGroup);
        joinButton = findViewById(R.id.joinButton);
        cancelButton = findViewById(R.id.cancelButton);
        institutionsListContainer = findViewById(R.id.institutionsListContainer);
        progressBar = findViewById(R.id.progressBar);
    }

    private void checkUserInstitution() {
        // Removed check - users can now join multiple institutions
        // Just log for debugging purposes
        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "User " + userId + " can join multiple institutions");
    }

    private void setupListeners() {
        // Search button click
        searchButton.setOnClickListener(v -> {
            if (validateInstitutionId()) {
                searchInstitution();
            }
        });

        // Join button click
        joinButton.setOnClickListener(v -> {
            if (validateRoleSelection()) {
                joinInstitution();
            }
        });

        // Cancel button click
        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private boolean validateInstitutionId() {
        String institutionName = institutionNameEditText.getText().toString().trim();

        if (institutionName.isEmpty()) {
            Toast.makeText(this, "Please enter Institution Name", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateRoleSelection() {
        int selectedId = rolesRadioGroup.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void searchInstitution() {
        String searchName = institutionNameEditText.getText().toString().trim();

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        institutionDetailsContainer.setVisibility(View.GONE);

        // Search institutions by name (case-insensitive)
        db.collection("institutions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    boolean found = false;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String institutionName = document.getString("institutionName");
                        
                        if (institutionName != null && institutionName.equalsIgnoreCase(searchName)) {
                            // Found matching institution
                            currentInstitutionId = document.getId();
                            currentInstitutionName = institutionName;
                            String managerRoleName = document.getString("managerRoleName");
                            List<String> roles = (List<String>) document.get("roles");

                            // Display institution details
                            institutionNameText.setText(currentInstitutionName);

                            // Clear previous radio buttons
                            rolesRadioGroup.removeAllViews();

                            // Add radio buttons for each role (exclude manager role)
                            if (roles != null && !roles.isEmpty()) {
                                for (String role : roles) {
                                    // Skip manager role - only regular users can join
                                    if (role.equalsIgnoreCase(managerRoleName)) {
                                        continue;
                                    }
                                    
                                    RadioButton radioButton = new RadioButton(this);
                                    radioButton.setText(role);
                                    radioButton.setTextSize(16);
                                    radioButton.setTextColor(android.graphics.Color.parseColor("#212121"));
                                    radioButton.setPadding(16, 16, 16, 16);
                                    rolesRadioGroup.addView(radioButton);
                                }
                                
                                if (rolesRadioGroup.getChildCount() == 0) {
                                    Toast.makeText(this, "No roles available for users to join", 
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } else {
                                Toast.makeText(this, "No roles available in this institution", 
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Show institution details container
                            institutionDetailsContainer.setVisibility(View.VISIBLE);

                            Log.d(TAG, "Institution found: " + currentInstitutionName);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Toast.makeText(this, "Institution not found. Please check the name.", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error searching institution", e);
                    Toast.makeText(this, "Error searching institution: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllInstitutions() {
        db.collection("institutions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    institutionsListContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView noInstitutionsText = new TextView(this);
                        noInstitutionsText.setText("No institutions available yet.");
                        noInstitutionsText.setTextSize(14);
                        noInstitutionsText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        noInstitutionsText.setPadding(16, 16, 16, 16);
                        institutionsListContainer.addView(noInstitutionsText);
                        return;
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String institutionId = document.getId();
                        String institutionName = document.getString("institutionName");
                        String managerRoleName = document.getString("managerRoleName");
                        List<String> roles = (List<String>) document.get("roles");

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
                        nameText.setId(View.generateViewId());
                        nameText.setText(institutionName);
                        nameText.setTextSize(18);
                        nameText.setTextColor(android.graphics.Color.parseColor("#212121"));
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
                        managerText.setId(View.generateViewId());
                        managerText.setText("Manager Role: " + (managerRoleName != null ? managerRoleName : "N/A"));
                        managerText.setTextSize(14);
                        managerText.setTextColor(android.graphics.Color.parseColor("#757575"));
                        
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams managerParams = 
                                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        managerParams.topToBottom = nameText.getId();
                        managerParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                        managerParams.topMargin = 8;
                        managerText.setLayoutParams(managerParams);
                        institutionCard.addView(managerText);

                        // Available Roles
                        TextView rolesText = new TextView(this);
                        rolesText.setId(View.generateViewId());
                        String rolesString = roles != null && !roles.isEmpty() ? 
                                "Roles: " + String.join(", ", roles) : "No roles available";
                        rolesText.setText(rolesString);
                        rolesText.setTextSize(14);
                        rolesText.setTextColor(android.graphics.Color.parseColor("#757575"));
                        
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams rolesParams = 
                                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                        0,
                                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        rolesParams.topToBottom = managerText.getId();
                        rolesParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                        rolesParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                        rolesParams.topMargin = 4;
                        rolesText.setLayoutParams(rolesParams);
                        institutionCard.addView(rolesText);

                        // Make the card clickable
                        final String finalInstitutionId = institutionId;
                        final String finalInstitutionName = institutionName;
                        final List<String> finalRoles = roles;
                        
                        institutionCard.setOnClickListener(v -> {
                            // Set the search field with institution name
                            institutionNameEditText.setText(finalInstitutionName);
                            
                            // Display institution details
                            currentInstitutionId = finalInstitutionId;
                            currentInstitutionName = finalInstitutionName;
                            
                            institutionNameText.setText(finalInstitutionName);

                            // Clear previous radio buttons
                            rolesRadioGroup.removeAllViews();

                            // Add radio buttons for each role (exclude manager role)
                            if (finalRoles != null && !finalRoles.isEmpty()) {
                                for (String role : finalRoles) {
                                    // Skip manager role - only regular users can join
                                    if (role.equalsIgnoreCase(managerRoleName)) {
                                        continue;
                                    }
                                    
                                    RadioButton radioButton = new RadioButton(this);
                                    radioButton.setText(role);
                                    radioButton.setTextSize(16);
                                    radioButton.setTextColor(android.graphics.Color.parseColor("#212121"));
                                    radioButton.setPadding(16, 16, 16, 16);
                                    rolesRadioGroup.addView(radioButton);
                                }
                                
                                if (rolesRadioGroup.getChildCount() == 0) {
                                    Toast.makeText(this, "No roles available for users to join", 
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                // Show institution details container
                                institutionDetailsContainer.setVisibility(View.VISIBLE);
                                
                                // Scroll to top to show the details
                                if (mainScrollView != null) {
                                    mainScrollView.post(() -> mainScrollView.smoothScrollTo(0, 0));
                                }
                            } else {
                                Toast.makeText(this, "No roles available in this institution", 
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                        institutionsListContainer.addView(institutionCard);
                    }

                    Log.d(TAG, "Loaded " + queryDocumentSnapshots.size() + " institutions");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading institutions", e);
                    TextView errorText = new TextView(this);
                    errorText.setText("Error loading institutions");
                    errorText.setTextSize(14);
                    errorText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    errorText.setPadding(16, 16, 16, 16);
                    institutionsListContainer.addView(errorText);
                });
    }

    private void joinInstitution() {
        String userId = mAuth.getCurrentUser().getUid();
        int selectedId = rolesRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        String selectedRole = selectedRadioButton.getText().toString();

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // First, get current user's institutions array
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> institutions = new ArrayList<>();
                    
                    // Get existing institutions if any
                    if (documentSnapshot.exists() && documentSnapshot.contains("institutions")) {
                        List<Map<String, Object>> existingInstitutions = 
                                (List<Map<String, Object>>) documentSnapshot.get("institutions");
                        if (existingInstitutions != null) {
                            institutions.addAll(existingInstitutions);
                            
                            // Check if user already joined this specific institution
                            for (Map<String, Object> inst : existingInstitutions) {
                                String instId = (String) inst.get("institutionId");
                                if (instId != null && instId.equals(currentInstitutionId)) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "You have already joined this institution!", 
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }
                    }
                    
                    // Add new institution
                    Map<String, Object> newInstitution = new HashMap<>();
                    newInstitution.put("institutionId", currentInstitutionId);
                    newInstitution.put("role", selectedRole);
                    newInstitution.put("isManager", false);
                    institutions.add(newInstitution);
                    
                    // Update user document with institutions array
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("institutions", institutions);
                    updates.put("userRole", selectedRole); // Keep for backwards compatibility
                    updates.put("institutionId", currentInstitutionId); // Keep for backwards compatibility

                    db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Successfully joined " + currentInstitutionName + 
                                        " as " + selectedRole, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "User joined institution: " + currentInstitutionName + 
                                        " as " + selectedRole);
                                
                                // Go back to dashboard
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Error joining institution", e);
                                Toast.makeText(this, "Error joining institution: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
