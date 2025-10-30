package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateInstitutionActivity extends AppCompatActivity {

    private static final String TAG = "CreateInstitution";

    // UI Components
    private EditText institutionNameEditText;
    private EditText rolesEditText;
    private Button createButton;
    private Button cancelButton;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_institution);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameEditText = findViewById(R.id.institutionNameEditText);
        rolesEditText = findViewById(R.id.rolesEditText);
        createButton = findViewById(R.id.createButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        createButton.setOnClickListener(v -> handleCreateInstitution());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void handleCreateInstitution() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        String institutionName = institutionNameEditText.getText().toString().trim();
        String rolesInput = rolesEditText.getText().toString().trim();

        // Parse roles (comma-separated)
        List<String> rolesList = parseRoles(rolesInput);

        if (rolesList.isEmpty()) {
            Toast.makeText(this, "Please enter at least one role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        createButton.setEnabled(false);

        // Get current user info
        String managerId = mAuth.getCurrentUser().getUid();

        // First role becomes the manager's role
        String managerRoleName = rolesList.get(0);
        
        // Create institution with first role as manager role
        createInstitutionInFirestore(institutionName, rolesList, managerId, managerRoleName);
    }

    private void createInstitutionInFirestore(String institutionName, List<String> roles, 
                                              String managerId, String managerRoleName) {
        // Create institution document with auto-generated ID
        Map<String, Object> institution = new HashMap<>();
        institution.put("institutionName", institutionName);
        
        // Support multiple managers - store as array
        List<String> managerIds = new ArrayList<>();
        managerIds.add(managerId);
        institution.put("managerIds", managerIds);
        institution.put("managerId", managerId); // Keep for backwards compatibility
        
        institution.put("managerRoleName", managerRoleName);
        institution.put("roles", roles);
        institution.put("createdAt", System.currentTimeMillis());

        // Add to Firestore with auto-generated ID
        db.collection("institutions")
                .add(institution)
                .addOnSuccessListener(documentReference -> {
                    String institutionId = documentReference.getId();
                    Log.d(TAG, "Institution created with ID: " + institutionId);
                    
                    // Update manager's user document with institutionId
                    updateManagerInstitution(institutionId, managerId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    createButton.setEnabled(true);
                    Log.e(TAG, "Error creating institution", e);
                    Toast.makeText(this, "Error creating institution: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateManagerInstitution(String institutionId, String managerId) {
        // Get the first role from the institution document to set as manager's roleName
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String managerRoleName = documentSnapshot.getString("managerRoleName");
                        
                        // First, get current user's institutions array
                        db.collection("users").document(managerId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    List<Map<String, Object>> institutions = new ArrayList<>();
                                    
                                    // Get existing institutions if any
                                    if (userDoc.exists() && userDoc.contains("institutions")) {
                                        List<Map<String, Object>> existingInstitutions = 
                                                (List<Map<String, Object>>) userDoc.get("institutions");
                                        if (existingInstitutions != null) {
                                            institutions.addAll(existingInstitutions);
                                        }
                                    }
                                    
                                    // Add new institution with manager role
                                    Map<String, Object> newInstitution = new HashMap<>();
                                    newInstitution.put("institutionId", institutionId);
                                    newInstitution.put("role", managerRoleName);
                                    newInstitution.put("isManager", true);
                                    institutions.add(newInstitution);
                                    
                                    // Update user document with institutions array
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("institutions", institutions);
                                    updates.put("roleName", managerRoleName); // Keep for backwards compatibility
                                    updates.put("institutionId", institutionId); // Keep for backwards compatibility

                                    db.collection("users").document(managerId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                progressBar.setVisibility(View.GONE);
                                                createButton.setEnabled(true);
                                                Log.d(TAG, "Manager's institutions updated successfully");
                                                Toast.makeText(this, "Institution created successfully!", 
                                                        Toast.LENGTH_SHORT).show();
                                                
                                                // Go back to dashboard
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                progressBar.setVisibility(View.GONE);
                                                createButton.setEnabled(true);
                                                Log.e(TAG, "Error updating manager's institution", e);
                                                Toast.makeText(this, "Institution created but error updating your profile: " 
                                                        + e.getMessage(), Toast.LENGTH_LONG).show();
                                                
                                                // Still go back since institution was created
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    createButton.setEnabled(true);
                                    Log.e(TAG, "Error fetching user data", e);
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    createButton.setEnabled(true);
                    Log.e(TAG, "Error fetching institution data", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
        String institutionName = institutionNameEditText.getText().toString().trim();
        String rolesInput = rolesEditText.getText().toString().trim();

        if (institutionName.isEmpty()) {
            Toast.makeText(this, "Please enter institution name", Toast.LENGTH_SHORT).show();
            institutionNameEditText.requestFocus();
            return false;
        }

        if (rolesInput.isEmpty()) {
            Toast.makeText(this, "Please enter at least one role", Toast.LENGTH_SHORT).show();
            rolesEditText.requestFocus();
            return false;
        }

        return true;
    }

    private List<String> parseRoles(String rolesInput) {
        List<String> rolesList = new ArrayList<>();
        
        // Split by comma and trim each role
        String[] rolesArray = rolesInput.split(",");
        for (String role : rolesArray) {
            String trimmedRole = role.trim();
            if (!trimmedRole.isEmpty()) {
                rolesList.add(trimmedRole);
            }
        }
        
        return rolesList;
    }
}
